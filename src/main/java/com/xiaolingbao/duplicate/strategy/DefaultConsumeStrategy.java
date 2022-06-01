package com.xiaolingbao.duplicate.strategy;

import lombok.AllArgsConstructor;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 9:47
 * @description: 默认策略不进行去重
 */

@AllArgsConstructor
public class DefaultConsumeStrategy implements ConsumeStrategy {

    @Override
    public boolean invoke(Function<MessageExt, Boolean> consumeCallback, MessageExt messageExt) {
        return consumeCallback.apply(messageExt);
    }
}
