package com.xiaolingbao.model;

import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 15:46
 * @description: 
 */
public class QueueStatInfo {
    private String brokerName;
    private int queueId;
    private long brokerOffset;
    private long consumerOffset;
    private String clientInfo;
    private long lastTimestamp;

    public static QueueStatInfo getQueueStatInfo(MessageQueue messageQueue, OffsetWrapper offsetWrapper) {
        QueueStatInfo queueStatInfo = new QueueStatInfo();
        queueStatInfo.setBrokerName(messageQueue.getBrokerName());
        queueStatInfo.setQueueId(messageQueue.getQueueId());
        queueStatInfo.setBrokerOffset(offsetWrapper.getBrokerOffset());
        queueStatInfo.setConsumerOffset(offsetWrapper.getConsumerOffset());
        queueStatInfo.setLastTimestamp(offsetWrapper.getLastTimestamp());
        return queueStatInfo;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getBrokerOffset() {
        return brokerOffset;
    }

    public void setBrokerOffset(long brokerOffset) {
        this.brokerOffset = brokerOffset;
    }

    public long getConsumerOffset() {
        return consumerOffset;
    }

    public void setConsumerOffset(long consumerOffset) {
        this.consumerOffset = consumerOffset;
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
