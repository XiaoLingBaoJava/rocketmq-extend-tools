package com.xiaolingbao.service;

import com.xiaolingbao.model.TopicConsumerInfo;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.TopicList;

import java.util.List;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 14:26
 * @description: 
 */
public interface TopicService {

    /**
     * @author: xiaolingbao
     * @date: 2022/5/19 14:40
     * @param topic
     * @return java.util.Map<java.lang.String,java.util.Map<org.apache.rocketmq.common.message.MessageQueue,java.lang.String>>
     * @description: Map<ConsumerGroup, Map<Queue, Consumer>> 返回对于某个topic,订阅它的不同consumerGroup中queue与consumer的对应关系
     */
    Map<String, Map<MessageQueue, String>> getQueueAndConsumerByTopic(String topic);

    Map<String, TopicConsumerInfo> queryConsumeStatsListByTopicName(String topic);

    List<TopicConsumerInfo> queryTopicConsumerInfoList(String topic, String groupName);

    TopicList fetchAllTopicList();
}
