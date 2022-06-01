package com.xiaolingbao.scaling.monitor.runnable;

import com.alibaba.fastjson.JSONObject;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.scaling.ScalingGroup;
import com.xiaolingbao.scaling.monitor.AbstractMonitor;
import com.xiaolingbao.scaling.monitor.MonitorMethod;
import com.xiaolingbao.utils.ShellUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: xiaolingbao
 * @date: 2022/5/29 16:00
 * @description: 
 */
@Getter
@Setter
public class CpuMonitorRunnable extends AbstractMonitor {

    private static final Log log = ClientLogger.getLog();

    private static AtomicLong maxCpuUseRate = new AtomicLong(0);

    private static AtomicLong minCpuUseRate = new AtomicLong(1000);

    private static AtomicLong cpuUseSum = new AtomicLong(0);

    private static AtomicLong statisticCount = new AtomicLong(0);

    private static AtomicLong currentCpuUseRate = new AtomicLong();

    private String cmd = "top -b -n1 | fgrep \"Cpu(s)\" | tail -1 | awk -F'id,' '{split($1, vs, \",\"); v=vs[length(vs)]; sub(/\\s+/, \"\", v);sub(/\\s+/, \"\", v); printf \"%s\\n\", 100-v; }'";

    private String os = "liunx";

    public CpuMonitorRunnable(String monitorName, String scalingGroupName, ScalingGroup scalingGroup,
                              String cron, long statisticsDurationSecond, MonitorMethod monitorMethod,
                              long triggerExpansionThreshold, long triggerShrinkThreshold, int triggerAfterHowManyTimes) {
        this.monitorName = monitorName;
        this.scalingGroupName = scalingGroupName;
        this.scalingGroup = scalingGroup;
        this.cron = cron;
        this.statisticsDurationSecond = statisticsDurationSecond;
        this.monitorMethod = monitorMethod;
        this.triggerExpansionThreshold = triggerExpansionThreshold;
        this.triggerShrinkThreshold = triggerShrinkThreshold;
        this.triggerAfterHowManyTimes = triggerAfterHowManyTimes;
    }

    @Override
    public void run() {
        LocalDateTime startTime = LocalDateTime.now();
        while (startTime.plusSeconds(statisticsDurationSecond).isAfter(LocalDateTime.now())) {
            long cpuStatUseRate = doRun();
            if (cpuStatUseRate == -1) {
                log.warn("获取cpu使用率失败, 将重新尝试获取");
                continue;
            }
            switch (monitorMethod) {
                case MAX: {
                    if (cpuStatUseRate > maxCpuUseRate.get()) {
                        maxCpuUseRate.compareAndSet(maxCpuUseRate.get(), cpuStatUseRate);
                    }
                    break;
                }
                case MIN: {
                    if (cpuStatUseRate < minCpuUseRate.get()) {
                        minCpuUseRate.compareAndSet(minCpuUseRate.get(), cpuStatUseRate);
                    }
                    break;
                }
                case AVG: {
                    cpuUseSum.addAndGet(cpuStatUseRate);
                    statisticCount.incrementAndGet();
                    break;
                }
                case CURRENT: {
                    currentCpuUseRate.compareAndSet(currentCpuUseRate.get(), cpuStatUseRate);
                    checkAndScaling();
                    break;
                }
                default: {
                    log.error("monitorMethod的值错误, monitorMethod: {}", monitorMethod);
                }
            }
        }
        if (monitorMethod != MonitorMethod.CURRENT) {
            checkAndScaling();
        }
        resetStat();
    }

    private void checkAndScaling() {
        boolean doExpansion = false;
        boolean doShrink = false;
        switch (monitorMethod) {
            case MAX: {
                if (maxCpuUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (maxCpuUseRate.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case MIN: {
                if (minCpuUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (minCpuUseRate.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case AVG: {
                long avgCpuUseRate = cpuUseSum.get() / statisticCount.get();
                if (avgCpuUseRate >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (avgCpuUseRate <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case CURRENT: {
                if (currentCpuUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (currentCpuUseRate.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            default: {
                log.error("monitorMethod的值错误, monitorMethod: {}", monitorMethod);
            }
        }
        if (doExpansion && reachExpansionThresholdCount.incrementAndGet() >= triggerAfterHowManyTimes) {
            scalingGroup.start();
            reachExpansionThresholdCount.compareAndSet(triggerAfterHowManyTimes, 0);
        }
        if (doShrink && reachShrinkThresholdCount.incrementAndGet() >= triggerAfterHowManyTimes) {
            scalingGroup.remove();
            reachShrinkThresholdCount.compareAndSet(triggerAfterHowManyTimes, 0);
        }
    }

    private void resetStat() {
        maxCpuUseRate.compareAndSet(maxCpuUseRate.get(), 0);
        minCpuUseRate.compareAndSet(minCpuUseRate.get(), 1000);
        cpuUseSum.compareAndSet(cpuUseSum.get(), 0);
        statisticCount.compareAndSet(statisticCount.get(), 0);
    }

    private long doRun() {
        JSONObject result = null;
        try {
            if (StringUtils.equals("liunx", os)) {
                result = ShellUtil.executeInLimitTime(cmd, 2);
            } else if (StringUtils.equals("windows", os)) {
                result = ShellUtil.executeAndGetExitStatusInWindows(cmd);
            }
        } catch (TimeoutException e) {
            log.warn("执行shell命令超时, cmd: {}", cmd);
            return -1;
        }
        if (result != null && ((StringUtils.equals("liunx", os) && result.getInteger("exitStatus") == 0) || StringUtils.equals("windows", os))) {
            String cpuUseRateStr = result.getString("out");
            return Long.parseLong(cpuUseRateStr);
        }
        return -1;
    }
}
