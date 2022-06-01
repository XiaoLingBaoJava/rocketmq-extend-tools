package com.xiaolingbao.scaling.monitor.runnable;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.scaling.ScalingGroup;
import com.xiaolingbao.scaling.monitor.AbstractMonitor;
import com.xiaolingbao.scaling.monitor.MonitorMethod;
import com.xiaolingbao.service.BrokerService;
import com.xiaolingbao.service.impl.BrokerServiceImpl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 9:15
 * @description: 
 */
public class ProduceTpsMonitor extends AbstractMonitor {

    private static final Log log = ClientLogger.getLog();

    private static AtomicLong maxProduceTps = new AtomicLong(0);

    private static AtomicLong minProduceTps = new AtomicLong(Long.MAX_VALUE);

    private static AtomicLong produceTpsSum = new AtomicLong(0);

    private static AtomicLong statisticCount = new AtomicLong(0);

    private static AtomicLong currentProduceTps = new AtomicLong();

    // 192.168.20.100:10911
    private final String brokerAddr;

    private static final BrokerService brokerService = new BrokerServiceImpl();

    public ProduceTpsMonitor(String monitorName, String scalingGroupName, ScalingGroup scalingGroup,
                              String cron, long statisticsDurationSecond, MonitorMethod monitorMethod,
                              long triggerExpansionThreshold, long triggerShrinkThreshold, int triggerAfterHowManyTimes, String brokerAddr) {
        this.monitorName = monitorName;
        this.scalingGroupName = scalingGroupName;
        this.scalingGroup = scalingGroup;
        this.cron = cron;
        this.statisticsDurationSecond = statisticsDurationSecond;
        this.monitorMethod = monitorMethod;
        this.triggerExpansionThreshold = triggerExpansionThreshold;
        this.triggerShrinkThreshold = triggerShrinkThreshold;
        this.triggerAfterHowManyTimes = triggerAfterHowManyTimes;
        this.brokerAddr = brokerAddr;
    }

    @Override
    public void run() {
        LocalDateTime startTime = LocalDateTime.now();
        while (startTime.plusSeconds(statisticsDurationSecond).isAfter(LocalDateTime.now())) {
            long produceStatTps = doRun();
            if (produceStatTps == -1) {
                log.warn("获取produce tps失败, 将重新尝试获取");
                continue;
            }
            switch (monitorMethod) {
                case MAX: {
                    if (produceStatTps > maxProduceTps.get()) {
                        maxProduceTps.compareAndSet(maxProduceTps.get(), produceStatTps);
                    }
                    break;
                }
                case MIN: {
                    if (produceStatTps < minProduceTps.get()) {
                        minProduceTps.compareAndSet(minProduceTps.get(), produceStatTps);
                    }
                    break;
                }
                case AVG: {
                    produceTpsSum.addAndGet(produceStatTps);
                    statisticCount.incrementAndGet();
                    break;
                }
                case CURRENT: {
                    currentProduceTps.compareAndSet(currentProduceTps.get(), produceStatTps);
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
                if (maxProduceTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (maxProduceTps.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case MIN: {
                if (minProduceTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (minProduceTps.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case AVG: {
                long avgCpuUseRate = produceTpsSum.get() / statisticCount.get();
                if (avgCpuUseRate >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (avgCpuUseRate <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case CURRENT: {
                if (currentProduceTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (currentProduceTps.get() <= triggerShrinkThreshold) {
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
        maxProduceTps.compareAndSet(maxProduceTps.get(), 0);
        minProduceTps.compareAndSet(minProduceTps.get(), Long.MAX_VALUE);
        produceTpsSum.compareAndSet(produceTpsSum.get(), 0);
        statisticCount.compareAndSet(statisticCount.get(), 0);
    }


    private long doRun() {
        Map<String, Long> tpsMap = brokerService.getProduceAndConsumeTpsByBrokerAddr(brokerAddr);
        if (tpsMap == null || tpsMap.get("produceTps") == null) {
            log.warn("获取produceTps失败, brokerAddr: {}", brokerAddr);
            return -1;
        }
        return tpsMap.get("produceTps");
    }


}
