package com.xiaolingbao.service;

import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 11:06
 * @description: 
 */
public interface QueueService {

    /**
     * @author: xiaolingbao
     * @date: 2022/5/19 14:37
     * @param consumerGroupName
     * @return java.util.Map<org.apache.rocketmq.common.message.MessageQueue,java.lang.String>
     * @description: 返回某个consumerGroup中Queue与Consumer的对应关系
     */
    Map<MessageQueue, String> getQueueConsumerRelationByConsumerGroup(String consumerGroupName);

    Long getQueueConsumerOffset(MessageQueue messageQueue);
}
