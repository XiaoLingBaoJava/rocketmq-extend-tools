package com.xiaolingbao.scaling;

import com.xiaolingbao.scaling.monitor.MonitorManager;
import com.xiaolingbao.scaling.monitor.MonitorMethod;
import com.xiaolingbao.scaling.monitor.runnable.ProduceTpsMonitor;
import com.xiaolingbao.scaling.strategy.ProducerRemoveStrategy;
import org.junit.Test;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 20:20
 * @description: 
 */
public class ProducerScalingTest {

    @Test
    public void testScalingProducer() {
        ProducerStartingTemplate producerStartingTemplate = new ProducerStartingTemplate("SCALE_PRODUCER_GROUP",
                "192.168.20.100:9876", 0, 2);
        ProducerScalingGroup producerScalingGroup = new ProducerScalingGroup("PRODUCER_SCALING_GROUP", 0, 3, 5, producerStartingTemplate, new ProducerRemoveStrategy().getOldestRemoveStrategy());
        ProduceTpsMonitor produceTpsMonitor = new ProduceTpsMonitor("PRODUCE_TPS_MONITOR", "PRODUCER_SCALING_GROUP", producerScalingGroup,
                "0/5 * * * * ?", 3, MonitorMethod.AVG, 0, -1, 3, "192.168.20.100:10911");
        MonitorManager monitorManager = new MonitorManager();
        monitorManager.startMonitor(produceTpsMonitor);

        while (true) {

        }
    }
}
