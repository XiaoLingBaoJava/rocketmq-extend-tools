package com.xiaolingbao.service.impl;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.model.QueueStatInfo;
import com.xiaolingbao.model.TopicConsumerInfo;
import com.xiaolingbao.service.CommonService;
import com.xiaolingbao.service.QueueService;
import com.xiaolingbao.service.TopicService;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 11:09
 * @description: 
 */
public class QueueServiceImpl extends CommonService implements QueueService {

    private static final TopicService topicService = new TopicServiceImpl();

    private static final Log log = ClientLogger.getLog();

    @Override
    public Map<MessageQueue, String> getQueueConsumerRelationByConsumerGroup(String consumerGroupName) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        Map<MessageQueue, String> queueConsumerRelation = new HashMap<>();
        try {
            ConsumerConnection consumerConnection = mqAdminExt.examineConsumerConnectionInfo(consumerGroupName);
            for (Connection connection : consumerConnection.getConnectionSet()) {
                String clientId = connection.getClientId();
                ConsumerRunningInfo consumerRunningInfo = mqAdminExt.getConsumerRunningInfo(consumerGroupName, clientId, false);
                for (MessageQueue messageQueue : consumerRunningInfo.getMqTable().keySet()) {
                    queueConsumerRelation.put(messageQueue, clientId);
                }
            }
        } catch (Exception exception) {
            log.error("通过消费者组名称获取Queue与其对应的Consumer失败, 消费者组名: {}", consumerGroupName, exception);
        }
        mqAdminExt.shutdown();
        return queueConsumerRelation;
    }

    @Override
    public Long getQueueConsumerOffset(MessageQueue messageQueue) {
        Map<String, TopicConsumerInfo> topicConsumerInfoMap = topicService.queryConsumeStatsListByTopicName(messageQueue.getTopic());
        try {
            if (topicConsumerInfoMap != null) {
                for (TopicConsumerInfo topicConsumerInfo : topicConsumerInfoMap.values()) {
                    for (QueueStatInfo queueStatInfo : topicConsumerInfo.getQueueStatInfoList()) {
                        if (messageQueue.equals(new MessageQueue(topicConsumerInfo.getTopic(), queueStatInfo.getBrokerName(), queueStatInfo.getQueueId()))) {
                            return queueStatInfo.getConsumerOffset();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用getQueueConsumerOffset获取队列的consumeOffset失败, e");
        }
        return null;
    }


}
