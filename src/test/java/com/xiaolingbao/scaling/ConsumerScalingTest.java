package com.xiaolingbao.scaling;

import com.xiaolingbao.scaling.monitor.MonitorManager;
import com.xiaolingbao.scaling.monitor.MonitorMethod;
import com.xiaolingbao.scaling.monitor.runnable.CpuMonitorRunnable;
import com.xiaolingbao.scaling.strategy.ConsumerRemoveStrategy;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/30 16:17
 * @description: 
 */
public class ConsumerScalingTest {

    @Test
    public void testScalingConsumer() {
        Map<String, String> topicAndTag = new HashMap<>();
        topicAndTag.put("SCALE", "*");
        ConsumerStartingTemplate consumerStartingTemplate = new ConsumerStartingTemplate("SCALE_TEST_CONSUMER_GROUP", "192.168.20.100:9876", ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET,
                topicAndTag, MessageModel.CLUSTERING, new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 返回消费状态:消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        ConsumerScalingGroup consumerScalingGroup = new ConsumerScalingGroup("CONSUMER-SCALE", 0, 3, 5,
                                                        consumerStartingTemplate, new ConsumerRemoveStrategy().getOldestRemoveStrategy());
        CpuMonitorRunnable cpuMonitorRunnable = new CpuMonitorRunnable("CPU-CONSUMER-MONITER", "CONSUMER-SCALE", consumerScalingGroup,
                                                    "0/5 * * * * ?", 3, MonitorMethod.AVG, 80, 20, 3);
        cpuMonitorRunnable.setOs("windows");
        cpuMonitorRunnable.setCmd("echo 90");
//        cpuMonitorRunnable.setCmd("echo 10");
        MonitorManager monitorManager = new MonitorManager();
        monitorManager.startMonitor(cpuMonitorRunnable);

        while (true) {

        }
    }
}
