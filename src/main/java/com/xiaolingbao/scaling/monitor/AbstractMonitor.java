package com.xiaolingbao.scaling.monitor;

import com.xiaolingbao.scaling.ScalingGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: xiaolingbao
 * @date: 2022/5/29 8:18
 * @description: 
 */
@Getter
@Setter
public class AbstractMonitor implements Runnable {

    protected String monitorName;

    protected String scalingGroupName;

    protected ScalingGroup scalingGroup;

    protected String cron;

    protected long statisticsDurationSecond;

    protected MonitorMethod monitorMethod;

    protected long triggerExpansionThreshold;

    protected long triggerShrinkThreshold;

    protected int triggerAfterHowManyTimes;

    protected AtomicInteger reachExpansionThresholdCount = new AtomicInteger(0);

    protected AtomicInteger reachShrinkThresholdCount = new AtomicInteger(0);

    @Override
    public void run() {

    }
}
