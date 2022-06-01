package com.xiaolingbao.service.impl;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.service.MessageService;
import com.xiaolingbao.service.QueueService;
import org.apache.rocketmq.client.producer.SendResult;

/**
 * @author: xiaolingbao
 * @date: 2022/5/18 20:24
 * @description: 
 */
public class MessageServiceImpl implements MessageService {

    private static final Log log = ClientLogger.getLog();

    private static final QueueService queueServiceImpl = new QueueServiceImpl();

    @Override
    public Long getMessageLogicPositionInQueueAfterSend(SendResult sendResult) {
        Long consumeOffset = queueServiceImpl.getQueueConsumerOffset(sendResult.getMessageQueue());
        if (consumeOffset == null) {
            log.error("通过SendResult获取消息在队列中的逻辑位置(brokerOffset - consumeOffser)失败, sendResult: {}", sendResult.toString());
            return null;
        }
        // 若小于0,是因为消费速度很快，生产者生产完消息后马上被消费，consumeOffset早已更新,因此相减为负
        return sendResult.getQueueOffset() - consumeOffset < 0 ? 0 : sendResult.getQueueOffset() - consumeOffset;
    }
}
