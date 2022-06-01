package com.xiaolingbao.service.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.model.QueueStatInfo;
import com.xiaolingbao.model.TopicConsumerInfo;
import com.xiaolingbao.service.CommonService;
import com.xiaolingbao.service.QueueService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 14:30
 * @description: 
 */
public class TopicServiceImpl extends CommonService implements com.xiaolingbao.service.TopicService {

    private static final QueueService queueService = new QueueServiceImpl();

    private static final Log log = ClientLogger.getLog();

    @Override
    public Map<String, Map<MessageQueue, String>> getQueueAndConsumerByTopic(String topic) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        Map<String, Map<MessageQueue, String>> queueAndConsumerTable = new HashMap<>();
        try {
            GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);
            for (String group : groupList.getGroupList()) {
                queueAndConsumerTable.put(group, queueService.getQueueConsumerRelationByConsumerGroup(group));
            }
            return queueAndConsumerTable;
        } catch (Exception e) {
            log.error("通过topic获取queue和consumer的对应关系失败, topic: {}", topic, e);
        }
        mqAdminExt.shutdown();
        return null;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/19 20:40
     * @param topic
     * @return Map<ConsumeGroupName, TopicConsumerInfo>
     * @description:
     */
    @Override
    public Map<String, TopicConsumerInfo> queryConsumeStatsListByTopicName(String topic) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        Map<String, TopicConsumerInfo> consumeStatsMap = new HashMap<>();
        try {
            GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);
            for (String group : groupList.getGroupList()) {
                List<TopicConsumerInfo> topicConsumerInfoList = null;
                try {
                    topicConsumerInfoList = queryTopicConsumerInfoList(topic, group);
                } catch (Exception e) {
                    log.error("调用queryTopicConsumerInfoList方法失败, topic: {}, group: {}", topic, group, e);
                    mqAdminExt.shutdown();
                    return null;
                }
                consumeStatsMap.put(group, CollectionUtils.isEmpty(topicConsumerInfoList) ? new TopicConsumerInfo(topic) : topicConsumerInfoList.get(0));
            }
        } catch (Exception e) {
            log.error("调用queryConsumeStatsListByTopicName方法失败, topic: {}", topic, e);
            mqAdminExt.shutdown();
            return null;
        }
        mqAdminExt.shutdown();
        return consumeStatsMap;
    }

    @Override
    public List<TopicConsumerInfo> queryTopicConsumerInfoList(final String topic, String groupName) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        ConsumeStats consumeStats = null;
        try {
            consumeStats = mqAdminExt.examineConsumeStats(groupName, topic);
        } catch (Exception e) {
            log.error("调用examineConsumeStats方法失败, topic: {}, groupName: {}", topic, groupName);
            mqAdminExt.shutdown();
            return null;
        }
        List<MessageQueue> mqList = Lists.newArrayList(Iterables.filter(consumeStats.getOffsetTable().keySet(), new Predicate<MessageQueue>() {
            @Override
            public boolean apply(MessageQueue messageQueue) {
                return StringUtils.isBlank(topic) || messageQueue.getTopic().equals(topic);
            }
        }));
        Collections.sort(mqList);
        List<TopicConsumerInfo> topicConsumerInfoList = Lists.newArrayList();
        TopicConsumerInfo nowTopicConsumerInfo = null;
        Map<MessageQueue, String> messageQueueClientMap = queueService.getQueueConsumerRelationByConsumerGroup(groupName);
        for (MessageQueue mq: mqList) {
            if (nowTopicConsumerInfo == null || (!StringUtils.equals(mq.getTopic(), nowTopicConsumerInfo.getTopic()))) {
                nowTopicConsumerInfo = new TopicConsumerInfo(mq.getTopic());
                topicConsumerInfoList.add(nowTopicConsumerInfo);
            }
            QueueStatInfo queueStatInfo = QueueStatInfo.getQueueStatInfo(mq, consumeStats.getOffsetTable().get(mq));
            queueStatInfo.setClientInfo(messageQueueClientMap.get(mq));
            nowTopicConsumerInfo.appendQueueStatInfo(queueStatInfo);
        }

        mqAdminExt.shutdown();
        return topicConsumerInfoList;
    }

    @Override
    public TopicList fetchAllTopicList() {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        try {
            TopicList topicList = mqAdminExt.fetchAllTopicList();
            mqAdminExt.shutdown();
            return topicList;
        } catch (Exception e) {
            log.error("调用fetchAllTopicList获取所有topic失败", e);
            mqAdminExt.shutdown();
            return null;
        }
    }
}
