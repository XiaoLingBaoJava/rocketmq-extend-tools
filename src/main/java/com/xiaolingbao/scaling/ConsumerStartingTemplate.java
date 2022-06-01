package com.xiaolingbao.scaling;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: xiaolingbao
 * @date: 2022/5/28 17:12
 * @description: 
 */

@Getter
@Setter
public class ConsumerStartingTemplate implements StartingTemplate<DefaultMQPushConsumer> {

    private static final Log log = ClientLogger.getLog();

    private String consumerGroup;

    private String nameServerAddr;

    private ConsumeFromWhere consumeFromWhere;

    private Map<String, String> topicAndTag;

    private MessageModel messageModel;

    private MessageListenerConcurrently messageListenerConcurrently;

    private String instanceNamePrefix;

    private AtomicInteger number;

    public ConsumerStartingTemplate(String consumerGroup, String nameServerAddr, ConsumeFromWhere consumeFromWhere,
                                    Map<String, String> topicAndTag, MessageModel messageModel, MessageListenerConcurrently messageListenerConcurrently) {
        this.consumerGroup = consumerGroup;
        this.nameServerAddr = nameServerAddr;
        this.consumeFromWhere = consumeFromWhere;
        this.topicAndTag = topicAndTag;
        this.messageModel = messageModel;
        this.messageListenerConcurrently = messageListenerConcurrently;
        this.instanceNamePrefix = consumerGroup;
        this.number = new AtomicInteger(0);
    }


    @Override
    public DefaultMQPushConsumer start() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServerAddr);
        consumer.setConsumeFromWhere(consumeFromWhere);
        for (Map.Entry<String, String> entry : topicAndTag.entrySet()) {
            try {
                consumer.subscribe(entry.getKey(), entry.getValue());
            } catch (MQClientException e) {
                log.error("consumer订阅topic失败, topic: {}, tag: {}", entry.getKey(), entry.getValue(), e);
                return null;
            }
        }
        consumer.setMessageModel(messageModel);
        consumer.registerMessageListener(messageListenerConcurrently);
        consumer.setInstanceName(instanceNamePrefix + "_" + number.incrementAndGet());
        try {
            consumer.start();
        } catch (MQClientException e) {
            log.error("consumer启动失败, consumer: {}", consumer.toString(), e);
            return null;
        }
        return consumer;
    }
}
