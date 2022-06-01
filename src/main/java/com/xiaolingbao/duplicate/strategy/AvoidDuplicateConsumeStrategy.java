package com.xiaolingbao.duplicate.strategy;

import com.xiaolingbao.duplicate.DuplicateConfig;
import com.xiaolingbao.duplicate.persistence.JDBCPersistence;
import com.xiaolingbao.duplicate.persistence.Persist;
import com.xiaolingbao.duplicate.persistence.PersistenceElement;
import com.xiaolingbao.duplicate.persistence.RedisPersistence;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 9:46
 * @description: 去重的消费策略，可以采用redis和/或MySQL进行去重
 *               1、如果已经消费过，则不会重复消费
 *               2、如果相同的消息正在消费中，会延迟消费，轮到自己时若之前在消费的消息已成功，则不会重复消费
 *                  若相同的消息一直在消费，由于需要避免消息丢失，即使前一个消息没消费结束依然会消费当前消息
 */

@AllArgsConstructor
public class AvoidDuplicateConsumeStrategy implements ConsumeStrategy {

    private final static Log log = ClientLogger.getLog();

    private final DuplicateConfig duplicateConfig;


    @Override
    public boolean invoke(Function<MessageExt, Boolean> consumeCallback, MessageExt messageExt) {
        return doInvoke(consumeCallback, messageExt);
    }

    private boolean doInvoke(Function<MessageExt, Boolean> consumeCallback, MessageExt messageExt) {
        RedisPersistence redisPersistence = duplicateConfig.getRedisPersistence();
        JDBCPersistence jdbcPersistence = duplicateConfig.getJdbcPersistence();

        PersistenceElement persistenceElement = new PersistenceElement(duplicateConfig.getApplicationName(), messageExt.getTopic(), messageExt.getTags() == null ? "" : messageExt.getTags(), duplicateConfig.getMsgDuplicateKey(messageExt, duplicateConfig.getMsgKeyStrategy()));

        Boolean shouldConsume = true;

        if (persistenceElement.getMsgUniqKey() != null) {
            if (redisPersistence != null) {
                shouldConsume = redisPersistence.doPersistence(persistenceElement, duplicateConfig.getDuplicateProcessingExpireMilliSeconds());
            }

            if (jdbcPersistence != null && shouldConsume) {
                shouldConsume = jdbcPersistence.doPersistence(persistenceElement, duplicateConfig.getDuplicateProcessingExpireMilliSeconds());
            }
        }

        if (shouldConsume != null && shouldConsume) {
            // 消费消息
            return doHandleMsgAndUpdateStatus(consumeCallback, messageExt, persistenceElement);
        } else {
            // 有与当前消息重复的消息已被消费过或正在被消费
            String consumeStatus = null;
            if (redisPersistence != null) {
                consumeStatus = redisPersistence.get(persistenceElement);
            }
            if (jdbcPersistence != null && StringUtils.isEmpty(consumeStatus)) {
                consumeStatus = jdbcPersistence.get(persistenceElement);
            }
            if (Persist.CONSUME_STATUS_CONSUMING.equals(consumeStatus)) {
                log.warn("有重复的消息正在消费中，当前消息会稍后再进行消费, persistenceElement: {}, msgId: {}", persistenceElement.toString(), messageExt.getMsgId());
                return false;
            } else if (Persist.CONSUME_STATUS_CONSUMED.equals(consumeStatus)) {
                log.warn("已经有重复的消息消费成功了，当前消息不会被消费，直接认为消费成功, persistenceElement: {}, msgId: {}", persistenceElement.toString(), messageExt.getMsgId());
                return true;
            } else {
                log.warn("出现UNKNOWN的情况，consumeStatus异常,为: {}, 将直接进行消费, persistenceElement: {}, msgId: {}", consumeStatus, persistenceElement.toString(), messageExt.getMsgId());
                return doHandleMsgAndUpdateStatus(consumeCallback, messageExt, persistenceElement);
            }
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/27 15:47
     * @param consumeCallback
     * @param messageExt
     * @param persistenceElement
     * @return boolean
     * @description: 消费消息并在幂等表中保存消费状态
     */
    private boolean doHandleMsgAndUpdateStatus(final Function<MessageExt, Boolean> consumeCallback, final MessageExt messageExt, final PersistenceElement persistenceElement) {
        if (persistenceElement.getMsgUniqKey() == null) {
            log.warn("幂等令牌MsgUniqKey为空, 该消息会被消费但无法保存消费状态, message: {}", messageExt.toString());
            return consumeCallback.apply(messageExt);
        } else {
            RedisPersistence redisPersistence = duplicateConfig.getRedisPersistence();
            JDBCPersistence jdbcPersistence = duplicateConfig.getJdbcPersistence();
            boolean consumeSuccess = false;
            try {
                consumeSuccess = consumeCallback.apply(messageExt);
            } catch (Throwable e) {
                // 消费失败,删除幂等表中的key
                try {
                    if (redisPersistence != null) {
                        redisPersistence.delete(persistenceElement);
                        log.debug("消费失败，删除redis幂等表中的key, messageExt: {}, persistenceElement: {}", messageExt.toString(), persistenceElement.toString());
                    }
                    if (jdbcPersistence != null) {
                        jdbcPersistence.delete(persistenceElement);
                        log.debug("消费失败，删除MySQL幂等表中的key, messageExt: {}, persistenceElement: {}", messageExt.toString(), persistenceElement.toString());
                    }
                } catch (Exception ex) {
                    log.error("删除幂等表中的key时发生错误, persistenceElement: {}", persistenceElement.toString(), ex);
                }
                throw e;
            }

            try {
                if (consumeSuccess) {
                    // 标记该消息已成功消费
                    log.debug("将消息状态设为已消费成功, persistenceElement: {}, msgId: {}", persistenceElement.toString(), messageExt.getMsgId());
                    if (redisPersistence != null) {
                        redisPersistence.markConsumed(persistenceElement, duplicateConfig.getDuplicateRecordReserverMinutes());
                    }
                    if (jdbcPersistence != null) {
                        jdbcPersistence.markConsumed(persistenceElement, duplicateConfig.getDuplicateRecordReserverMinutes());
                    }
                } else {
                    // 消费失败,删除幂等表的key
                    log.debug("消费失败,consumeSuccess为false, 删除幂等表的key, persistenceElement: {}", persistenceElement.toString());
                    if (redisPersistence != null) {
                        redisPersistence.delete(persistenceElement);
                    }
                    if (jdbcPersistence != null) {
                        jdbcPersistence.delete(persistenceElement);
                    }
                }
            } catch (Exception e) {
                log.error("标记消息是否消费成功操作发生异常, messageExt: {}", messageExt.toString(), e);
            }
            return consumeSuccess;
        }
    }
}
