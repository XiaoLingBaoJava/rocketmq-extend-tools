package com.xiaolingbao.duplicate.strategy;

import org.apache.rocketmq.common.message.MessageExt;

import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 9:43
 * @description: 
 */
public interface ConsumeStrategy {
    boolean invoke(Function<MessageExt, Boolean> consumeCallback, MessageExt messageExt);
}
