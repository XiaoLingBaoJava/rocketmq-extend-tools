package com.xiaolingbao.scaling.monitor.runnable;

import com.alibaba.fastjson.JSONObject;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.scaling.ScalingGroup;
import com.xiaolingbao.scaling.monitor.AbstractMonitor;
import com.xiaolingbao.scaling.monitor.MonitorMethod;
import com.xiaolingbao.utils.ShellUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 8:34
 * @description: 
 */
public class MemoryMonitorRunnable extends AbstractMonitor {
    private static final Log log = ClientLogger.getLog();

    private static AtomicLong maxMemoryUseRate = new AtomicLong(0);

    private static AtomicLong minMemoryUseRate = new AtomicLong(1000);

    private static AtomicLong memoryUseSum = new AtomicLong(0);

    private static AtomicLong statisticCount = new AtomicLong(0);

    private static AtomicLong currentMemoryUseRate = new AtomicLong();

    private String cmd = "free -m | awk -F '[ :]+' 'NR==2{printf \"%d\", ($2-$7)/$2*100}'";
    
    private String os = "liunx";

    public MemoryMonitorRunnable(String monitorName, String scalingGroupName, ScalingGroup scalingGroup,
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
            long memoryStatUseRate = doRun();
            if (memoryStatUseRate == -1) {
                log.warn("获取内存使用率失败, 将重新尝试获取");
                continue;
            }
            switch (monitorMethod) {
                case MAX: {
                    if (memoryStatUseRate > maxMemoryUseRate.get()) {
                        maxMemoryUseRate.compareAndSet(maxMemoryUseRate.get(), memoryStatUseRate);
                    }
                    break;
                }
                case MIN: {
                    if (memoryStatUseRate < minMemoryUseRate.get()) {
                        minMemoryUseRate.compareAndSet(minMemoryUseRate.get(), memoryStatUseRate);
                    }
                    break;
                }
                case AVG: {
                    memoryUseSum.addAndGet(memoryStatUseRate);
                    statisticCount.incrementAndGet();
                    break;
                }
                case CURRENT: {
                    currentMemoryUseRate.compareAndSet(currentMemoryUseRate.get(), memoryStatUseRate);
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
                if (maxMemoryUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (maxMemoryUseRate.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case MIN: {
                if (minMemoryUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (minMemoryUseRate.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case AVG: {
                long avgMemoryUseRate = memoryUseSum.get() / statisticCount.get();
                if (avgMemoryUseRate >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (avgMemoryUseRate <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case CURRENT: {
                if (currentMemoryUseRate.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (currentMemoryUseRate.get() <= triggerShrinkThreshold) {
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
        maxMemoryUseRate.compareAndSet(maxMemoryUseRate.get(), 0);
        minMemoryUseRate.compareAndSet(minMemoryUseRate.get(), 1000);
        memoryUseSum.compareAndSet(memoryUseSum.get(), 0);
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
