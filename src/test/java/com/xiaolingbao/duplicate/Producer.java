package com.xiaolingbao.duplicate;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.Test;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 19:55
 * @description: 
 */
public class Producer {

    @Test
    public void test() throws MQClientException, RemotingException, InterruptedException {
        DefaultMQProducer producer = new DefaultMQProducer("DUPLICATE-TEST-GROUP");
        producer.setNamesrvAddr("192.168.20.100:9876");

        producer.start();


        byte[] body = ("Hi").getBytes();

        for (int i = 0; i < 10; i++) {
            Message msg = new Message("DUPLICATE-TEST-TOPIC", "myTag", body);
            msg.setKeys("5");

            int index = i + 1;
            producer.send(msg, new SendCallback() {
                // 当producer接收到MQ发送来的ACK后，就会触发该回调方法的执行
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.println("发送第" + index + "条消息成功");
                }

                @Override
                public void onException(Throwable e) {
                    e.printStackTrace();
                }
            });
            Thread.sleep(1000);
        }


        Thread.sleep(10000);
        producer.shutdown();
    }
}
