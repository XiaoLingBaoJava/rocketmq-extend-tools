package com.xiaolingbao.scaling;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 10:27
 * @description: 
 */
@Getter
@Setter
public class ProducerStartingTemplate implements StartingTemplate<DefaultMQProducer> {

    private static final Log log = ClientLogger.getLog();

    private String producerGroupName;

    private String nameServerAddr;

    private int retryTimesWhenSendAsyncFailed;

    private int defaultTopicQueueNums;

    private String instanceNamePrefix;

    private AtomicInteger number;

    public ProducerStartingTemplate(String producerGroupName, String nameServerAddr, int retryTimesWhenSendAsyncFailed, int defaultTopicQueueNums) {
        this.producerGroupName = producerGroupName;
        this.nameServerAddr = nameServerAddr;
        this.retryTimesWhenSendAsyncFailed = retryTimesWhenSendAsyncFailed;
        this.defaultTopicQueueNums = defaultTopicQueueNums;
        this.instanceNamePrefix = producerGroupName;
        this.number = new AtomicInteger(0);
    }

    @Override
    public DefaultMQProducer start() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroupName);
        producer.setNamesrvAddr(nameServerAddr);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        producer.setDefaultTopicQueueNums(defaultTopicQueueNums);
        producer.setInstanceName(instanceNamePrefix + "_" + number.incrementAndGet());
        try {
            producer.start();
        } catch (MQClientException e) {
            log.error("启动producer失败, producer: {}", producer.toString(), e);
            return null;
        }
        return producer;
    }
}
