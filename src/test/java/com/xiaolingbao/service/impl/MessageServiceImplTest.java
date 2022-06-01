package com.xiaolingbao.service.impl;


import com.xiaolingbao.service.TopicService;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MessageServiceImplTest {

    private MessageServiceImpl messageService = new MessageServiceImpl();

    private TopicService topicService = new TopicServiceImpl();


    @Test
    public void testGetMessageLogicPositionInQueueAfterSend() throws MQClientException, RemotingException, InterruptedException, MQBrokerException {


        DefaultMQProducer producer = new DefaultMQProducer("newProducerGroup1");
        producer.setNamesrvAddr("192.168.20.100:9876");

        // 指定异步发送失败后不进行重试发送
        producer.setRetryTimesWhenSendAsyncFailed(0);

        // 指定新创建的Topic的Queue数量为2，默认为4
        producer.setDefaultTopicQueueNums(2);

        producer.start();


        byte[] body = ("Hi").getBytes();
        Message msg = new Message("newTopic1", "myTag", body);
        // 异步发送， 指定回调
        producer.send(msg, new SendCallback() {
            // 当producer接收到MQ发送来的ACK后，就会触发该回调方法的执行
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("first queueOffset:" + sendResult.getQueueOffset());
                Long logicPos = messageService.getMessageLogicPositionInQueueAfterSend(sendResult);
                System.out.println("first logicPos: " + logicPos);
            }

            @Override
            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });

        byte[] bodys = ("Hi" + 1).getBytes();
        Message message = new Message("newTopic1", "myTag", bodys);
        producer.send(message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("second Queue Offset: " + sendResult.getQueueOffset());
                Long logicPos = messageService.getMessageLogicPositionInQueueAfterSend(sendResult);
                System.out.println("sencond logic offset: " + logicPos);
            }

            @Override
            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });


        Thread.sleep(15000);
        producer.shutdown();
    }

    @Test
    public void batchTest() throws MQClientException, RemotingException, InterruptedException {
        DefaultMQProducer producer = new DefaultMQProducer("newProducerGroup1");
        producer.setNamesrvAddr("192.168.20.100:9876");

        // 指定异步发送失败后不进行重试发送
        producer.setRetryTimesWhenSendAsyncFailed(0);

        // 指定新创建的Topic的Queue数量为2，默认为4
        producer.setDefaultTopicQueueNums(2);

        producer.start();

        for (int i = 0; i < 20; i++) {
            byte[] body = ("Hi" + i).getBytes();
            Message msg = new Message("newTopic1", "myTag", body);
            // 异步发送， 指定回调
            int temp = i;
            producer.send(msg, new SendCallback() {
                // 当producer接收到MQ发送来的ACK后，就会触发该回调方法的执行
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.println("第" + temp + "个消息发送成功, " + "queueOffset:" + sendResult.getQueueOffset());
                    Long logicPos = messageService.getMessageLogicPositionInQueueAfterSend(sendResult);
                    System.out.println("第" + temp + "个消息发送成功, " + "logicPos: " + logicPos);
                }

                @Override
                public void onException(Throwable e) {
                    e.printStackTrace();
                }
            });
        }


        Thread.sleep(95000);
        producer.shutdown();
    }


}