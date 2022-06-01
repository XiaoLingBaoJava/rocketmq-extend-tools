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
 * @date: 2022/5/31 10:18
 * @description: 
 */
public class ConsumeTpsMonitor extends AbstractMonitor {

    private static final Log log = ClientLogger.getLog();

    private static AtomicLong maxConsumeTps = new AtomicLong(0);

    private static AtomicLong minConsumeTps = new AtomicLong(Long.MAX_VALUE);

    private static AtomicLong consumeTpsSum = new AtomicLong(0);

    private static AtomicLong statisticCount = new AtomicLong(0);

    private static AtomicLong currentConsumeTps = new AtomicLong();

    // 192.168.20.100:10911
    private final String brokerAddr;

    private static final BrokerService brokerService = new BrokerServiceImpl();

    public ConsumeTpsMonitor(String monitorName, String scalingGroupName, ScalingGroup scalingGroup,
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
            long consumeStatTps = doRun();
            if (consumeStatTps == -1) {
                log.warn("获取consume tps失败, 将重新尝试获取");
                continue;
            }
            switch (monitorMethod) {
                case MAX: {
                    if (consumeStatTps > maxConsumeTps.get()) {
                        maxConsumeTps.compareAndSet(maxConsumeTps.get(), consumeStatTps);
                    }
                    break;
                }
                case MIN: {
                    if (consumeStatTps < minConsumeTps.get()) {
                        minConsumeTps.compareAndSet(minConsumeTps.get(), consumeStatTps);
                    }
                    break;
                }
                case AVG: {
                    consumeTpsSum.addAndGet(consumeStatTps);
                    statisticCount.incrementAndGet();
                    break;
                }
                case CURRENT: {
                    currentConsumeTps.compareAndSet(currentConsumeTps.get(), consumeStatTps);
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
                if (maxConsumeTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (maxConsumeTps.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case MIN: {
                if (minConsumeTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (minConsumeTps.get() <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case AVG: {
                long avgConsumeTps = consumeTpsSum.get() / statisticCount.get();
                if (avgConsumeTps >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (avgConsumeTps <= triggerShrinkThreshold) {
                    doShrink = true;
                }
                break;
            }
            case CURRENT: {
                if (currentConsumeTps.get() >= triggerExpansionThreshold) {
                    doExpansion = true;
                }
                if (currentConsumeTps.get() <= triggerShrinkThreshold) {
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
        maxConsumeTps.compareAndSet(maxConsumeTps.get(), 0);
        minConsumeTps.compareAndSet(minConsumeTps.get(), Long.MAX_VALUE);
        consumeTpsSum.compareAndSet(consumeTpsSum.get(), 0);
        statisticCount.compareAndSet(statisticCount.get(), 0);
    }


    private long doRun() {
        Map<String, Long> tpsMap = brokerService.getProduceAndConsumeTpsByBrokerAddr(brokerAddr);
        if (tpsMap == null || tpsMap.get("consumeTps") == null) {
            log.warn("获取consumeTps失败, brokerAddr: {}", brokerAddr);
            return -1;
        }
        return tpsMap.get("consumeTps");
    }

}
