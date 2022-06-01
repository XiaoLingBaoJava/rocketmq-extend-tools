package com.xiaolingbao.service.impl;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author: xiaolingbao
 * @date: 2022/5/21 16:30
 * @description: 
 */
public class TestConsumer {

    private static DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("newGroup1");


    public static void main(String[] args) throws MQClientException {
        // 定义push消费者

        consumer.setInstanceName("newConsumer1");

        // 指定NameServer
        consumer.setNamesrvAddr("192.168.20.100:9876");

        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        // 指定消费topic与tag
        consumer.subscribe("newTopic1", "*");

        // 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            // 一旦Broker中有了其订阅的消息，就会触发该方法的执行,其返回值为当前consumer
            // 消费的状态
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    System.out.println("consume端所消费消息的queueOffset: " + msg.getQueueOffset() + " , queueId: " + msg.getQueueId());
                }
                // 返回消费状态:消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 开启消费者消费
        consumer.start();
        System.out.println("Consumer Started");
    }
}
