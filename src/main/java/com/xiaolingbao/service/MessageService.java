package com.xiaolingbao.service;

import org.apache.rocketmq.client.producer.SendResult;

/**
 * @author: xiaolingbao
 * @date: 2022/5/18 20:14
 * @description: 
 */
public interface MessageService {

    Long getMessageLogicPositionInQueueAfterSend(SendResult sendResult);
}
