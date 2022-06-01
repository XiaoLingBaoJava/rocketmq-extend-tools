package com.xiaolingbao.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: xiaolingbao
 * @date: 2022/5/19 15:29
 * @description: 
 */
public class TopicConsumerInfo {
    private String topic;

    private long diffTotal;

    private long lastTimestamp;

    private List<QueueStatInfo> queueStatInfoList = new ArrayList<>();

    public TopicConsumerInfo(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getDiffTotal() {
        return diffTotal;
    }

    public void setDiffTotal(long diffTotal) {
        this.diffTotal = diffTotal;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public List<QueueStatInfo> getQueueStatInfoList() {
        return queueStatInfoList;
    }

    public void appendQueueStatInfo(QueueStatInfo queueStatInfo) {
        queueStatInfoList.add(queueStatInfo);
        diffTotal = diffTotal + (queueStatInfo.getBrokerOffset() - queueStatInfo.getConsumerOffset());
        lastTimestamp = Math.max(lastTimestamp, queueStatInfo.getLastTimestamp());
    }
}
