package com.xiaolingbao.duplicate;

import com.xiaolingbao.duplicate.strategy.AvoidDuplicateConsumeStrategy;
import com.xiaolingbao.duplicate.strategy.ConsumeStrategy;
import com.xiaolingbao.duplicate.strategy.DefaultConsumeStrategy;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author: xiaolingbao
 * @date: 2022/5/21 20:21
 * @description: 带去重逻辑的Listener，自定义Listener需继承该抽象类,消费者需要注册自定义Listener
 */
public abstract class AvoidDuplicateMsgListener implements MessageListenerConcurrently {

    private static final Log log = ClientLogger.getLog();

    // 默认不去重
    private DuplicateConfig duplicateConfig = DuplicateConfig.disableAvoidDuplicateConsumeConfig("DEFAULT-CONSUMER-GROUP");

    // 默认不去重
    public AvoidDuplicateMsgListener() {
        log.info("采用默认方式构造AvoidDuplicateMsgListener,默认不去重,duplicateConfig: {}", duplicateConfig);
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/26 21:39
     * @param duplicateConfig
     * @return AvoidDuplicateMsgListener
     * @description: 配置去重策略
     */
    public AvoidDuplicateMsgListener(DuplicateConfig duplicateConfig) {
        this.duplicateConfig = duplicateConfig;
        log.info("在AvoidDuplicateMsgListener构造函数中配置去重策略,duplicateConfig: {}", duplicateConfig);
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/27 16:22
     * @param messageExt
     * @return true表示消费成功,false为消费失败
     * @description: 子类实现此方法，真正处理信息
     */
    protected abstract boolean doHandleMsg(final MessageExt messageExt);



    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        boolean isConsumeFail = false;
        int lastSuccessIndex = -1;
        for (int i = 0; i < msgs.size(); i++) {
            MessageExt msg = msgs.get(i);
            try {
                isConsumeFail = !handleMsgInner(msg);
            } catch (Exception e) {
                log.warn("在消费消息: {}时出现异常", msg, e);
                isConsumeFail = true;
            }

            // 如果出现消费失败则退出消费，之后会重发尚未消费的消息
            if (isConsumeFail) {
                break;
            } else {
                lastSuccessIndex = i;
            }
        }

        if (!isConsumeFail) {
            log.info("消费 [{}]条消息全部成功", msgs.size());
        } else {
            // 标记成功位，后面的会重发以重新消费，在这个位置之前的不会重发
            context.setAckIndex(lastSuccessIndex);
            log.warn("消费 [{}]条消息失败, 最后一个成功消息的index: {}", msgs.size(), context.getAckIndex());
        }

        // 无论如何最后都返回CONSUME_SUCCESS,这样就不会触发MQ自身的重试机制
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }


    /**
     * @author: xiaolingbao
     * @date: 2022/5/27 9:31
     * @param messageExt
     * @return boolean
     * @description: 消费消息，带去重逻辑
     */
    private boolean handleMsgInner(final MessageExt messageExt) {
        ConsumeStrategy strategy = new DefaultConsumeStrategy();

        if (duplicateConfig.getDuplicateStrategy() == DuplicateConfig.AVOID_DUPLICATE_ENABLE) {
            strategy = new AvoidDuplicateConsumeStrategy(duplicateConfig);
        }

        return strategy.invoke(AvoidDuplicateMsgListener.this::doHandleMsg, messageExt);
    }
}
