package com.xiaolingbao.service.client;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.utils.JsonUtil;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQAdminImpl;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.*;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.*;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.joor.Reflect;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author: xiaolingbao
 * @date: 2022/5/16 20:15
 * @description: MQAdminExtImpl提供对外暴露的API接口
 */
public class MQAdminExtImpl implements MQAdminExt {

    private final Log log = ClientLogger.getLog();

    public MQAdminExtImpl() {

    }

    public void init(long timeoutMillis) throws MQClientException {
        MQAdminInstance.initMQAdminInstance(timeoutMillis);
    }

    @Override
    public void start() throws MQClientException {

    }

    @Override
    public void shutdown() {
        MQAdminInstance.destroyMQAdminInstance();
    }

    @Override
    public void updateBrokerConfig(String brokerAddr, Properties properties) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateBrokerConfig(brokerAddr, properties);
    }

    @Override
    public Properties getBrokerConfig(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getBrokerConfig(brokerAddr);
    }

    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig topicConfig) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateTopicConfig(addr, topicConfig);
    }

    @Override
    public void createAndUpdatePlainAccessConfig(String addr, PlainAccessConfig plainAccessConfig) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdatePlainAccessConfig(addr, plainAccessConfig);
    }

    @Override
    public void deletePlainAccessConfig(String addr, String accessKey) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deletePlainAccessConfig(addr, accessKey);
    }

    @Override
    public void updateGlobalWhiteAddrConfig(String addr, String globalWhiteAddr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().updateGlobalWhiteAddrConfig(addr, globalWhiteAddr);
    }

    @Override
    public ClusterAclVersionInfo examineBrokerClusterAclVersionInfo(String addr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().examineBrokerClusterAclVersionInfo(addr);
    }

    @Override
    public AclConfig examineBrokerClusterAclConfig(String addr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().examineBrokerClusterAclConfig(addr);
    }

    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig subscriptionGroupConfig) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateSubscriptionGroupConfig(addr, subscriptionGroupConfig);
    }

    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception e) {
            log.error("调用remotingClient的invokeSync方法获取订阅Config失败, addr: {}", addr, e);
            return null;
        }
        if (response == null) {
            log.error("调用remotingClient的invokeSync方法获取订阅Config,返回的response为null, addr: {}", addr, new IllegalStateException());
            return null;
        }
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                SubscriptionGroupWrapper subscriptionGroupWrapper = RemotingSerializable.decode(response.getBody(), SubscriptionGroupWrapper.class);
                return subscriptionGroupWrapper.getSubscriptionGroupTable().get(group);
            } default: {
                log.error("调用remotingClient的invokeSync方法获取订阅Config,返回的response的code不为success, addr: {}", addr, new MQBrokerException(response.getCode(), response.getRemark()));
                return null;
            }
        }
    }

    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception e) {
            log.error("调用remotingClient的invokeSync方法获取Topic Config失败, addr: {}", addr, e);
            return null;
        }
        if (response == null) {
            log.error("调用remotingClient的invokeSync方法获取Topic Config,返回的response为null, addr: {}", addr, new IllegalStateException());
            return null;
        }
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                TopicConfigSerializeWrapper topicConfigSerializeWrapper = RemotingSerializable.decode(response.getBody(), TopicConfigSerializeWrapper.class);
                return topicConfigSerializeWrapper.getTopicConfigTable().get(topic);
            } default: {
                log.error("调用remotingClient的invokeSync方法获取Topic Config,返回的response的code不为success, addr: {}", addr, new MQBrokerException(response.getCode(), response.getRemark()));
                return null;
            }
        }
    }

    @Override
    public TopicStatsTable examineTopicStats(String topic) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineTopicStats(topic);
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, MQClientException, InterruptedException {
        TopicList topicList = MQAdminInstance.threadLocalMQAdminExt().fetchAllTopicList();
        log.debug("topicList = {}", JsonUtil.obj2String(topicList.getTopicList()));
        return topicList;
    }

    @Override
    public TopicList fetchTopicsByCLuster(String clusterName) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchTopicsByCLuster(clusterName);
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchBrokerRuntimeStats(brokerAddr);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumeStats(consumerGroup);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumeStats(consumerGroup, topic);
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo() throws InterruptedException, MQBrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return MQAdminInstance.threadLocalMQAdminExt().examineBrokerClusterInfo();
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().examineTopicRouteInfo(topic);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumerConnectionInfo(consumerGroup);
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineProducerConnectionInfo(producerGroup, topic);
    }

    @Override
    public List<String> getNameServerAddressList() {
        return MQAdminInstance.threadLocalMQAdminExt().getNameServerAddressList();
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().wipeWritePermOfBroker(namesrvAddr, brokerName);
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {
        MQAdminInstance.threadLocalMQAdminExt().putKVConfig(namespace, key, value);
    }

    @Override
    public String getKVConfig(String namespace, String key) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getKVConfig(namespace, key);
    }

    @Override
    public KVTable getKVListByNamespace(String namespace) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getKVListByNamespace(namespace);
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        log.info("删除broker中的topic, broker addrs: {}, topic: {}", JsonUtil.obj2String(addrs), topic);
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInBroker(addrs, topic);
    }

    @Override
    public void deleteTopicInNameServer(Set<String> addrs, String topic) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        log.info("删除NameServer中的topic, NameServer addrs: {}, topic: {}", JsonUtil.obj2String(addrs), topic);
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInNameServer(addrs, topic);
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteSubscriptionGroup(addr, groupName);
    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateKvConfig(namespace, key, value);
    }

    @Override
    public void deleteKvConfig(String namespace, String key) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteKvConfig(namespace, key);
    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().resetOffsetByTimestampOld(consumerGroup, topic, timestamp, force);
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().resetOffsetByTimestamp(topic, group, timestamp, isForce);
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().resetOffsetNew(consumerGroup, topic, timestamp);
    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group, String clientAddr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().getConsumeStatus(topic, group, clientAddr);
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createOrUpdateOrderConf(key, value, isCluster);
    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().queryTopicConsumeByWho(topic);
    }

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic, String group) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().queryConsumeTimeSpan(topic, group);
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanExpiredConsumerQueue(cluster);
    }

    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanExpiredConsumerQueueByAddr(addr);
    }

    @Override
    public boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopic(cluster);
    }

    @Override
    public boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopicByAddr(addr);
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getConsumerRunningInfo(consumerGroup, clientId, jstack);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().consumeMessageDirectly(consumerGroup, clientId, msgId);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic, String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().consumeMessageDirectly(consumerGroup, clientId, topic, msgId);
    }

    @Override
    public List<MessageTrack> messageTrackDetail(MessageExt messageExt) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().messageTrackDetail(messageExt);
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().cloneGroupOffset(srcGroup, destGroup, topic, isOffline);
    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().viewBrokerStatsData(brokerAddr, statsName, statsKey);
    }

    @Override
    public Set<String> getClusterList(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getClusterList(topic);
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override
    public Set<String> getTopicClusterList(String topic) throws InterruptedException, MQBrokerException, MQClientException, RemotingException {
        return MQAdminInstance.threadLocalMQAdminExt().getTopicClusterList(topic);
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getAllSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getAllTopicGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq, long offset) throws RemotingException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateConsumeOffset(brokerAddr, consumeGroup, mq, offset);
    }

    @Override
    public void updateNameServerConfig(Properties properties, List<String> list) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateNameServerConfig(properties, list);
    }

    @Override
    public Map<String, Properties> getNameServerConfig(List<String> list) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException, UnsupportedEncodingException {
        return MQAdminInstance.threadLocalMQAdminExt().getNameServerConfig(list);
    }

    @Override
    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic, int queueId, long index, int count, String consumerGroup) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().queryConsumeQueue(brokerAddr, topic, queueId, index, count, consumerGroup);
    }

    @Override
    public boolean resumeCheckHalfMessage(String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().resumeCheckHalfMessage(msgId);
    }

    @Override
    public boolean resumeCheckHalfMessage(String topic, String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().resumeCheckHalfMessage(topic, msgId);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createTopic(key, newTopic, queueNum);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().earliestMsgStoreTime(mq);
    }

    @Override
    public MessageExt viewMessage(String offsetMsgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().viewMessage(offsetMsgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public MessageExt viewMessage(String topic, String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        try {
            return viewMessage(msgId);
        } catch (Exception e) {
            log.warn("调用viewMessage方法发生异常, topic: {}, msgId: {}, 程序自动改为调用queryMessage方法", topic, msgId, e);
        }
        MQAdminImpl mqAdminImpl = MQAdminInstance.threadLocalMqClientInstance().getMQAdminImpl();
        QueryResult queryResult = Reflect.on(mqAdminImpl).call("queryMessage", topic, msgId, 32, MessageClientIDSetter.getNearlyTimeFromID(msgId).getTime() - 1000 * 60 * 60 * 13L, Long.MAX_VALUE, true).get();
        if (queryResult != null && queryResult.getMessageList() != null && queryResult.getMessageList().size() > 0) {
            return queryResult.getMessageList().get(0);
        } else {
            return null;
        }
    }
}
