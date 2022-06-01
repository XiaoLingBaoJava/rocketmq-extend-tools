package com.xiaolingbao.scaling.strategy;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/28 20:41
 * @description: 
 */
public class ConsumerRemoveStrategy implements RemoveStrategy<DefaultMQPushConsumer> {


    @Override
    public Function<List<DefaultMQPushConsumer>, DefaultMQPushConsumer> getNewestRemoveStrategy() {
        return new Function<List<DefaultMQPushConsumer>, DefaultMQPushConsumer>() {
            @Override
            public DefaultMQPushConsumer apply(List<DefaultMQPushConsumer> defaultMQPushConsumers) {
                return defaultMQPushConsumers.get(0);
            }
        };
    }

    @Override
    public Function<List<DefaultMQPushConsumer>, DefaultMQPushConsumer> getOldestRemoveStrategy() {
        return new Function<List<DefaultMQPushConsumer>, DefaultMQPushConsumer>() {
            @Override
            public DefaultMQPushConsumer apply(List<DefaultMQPushConsumer> defaultMQPushConsumers) {
                return defaultMQPushConsumers.get(defaultMQPushConsumers.size() - 1);
            }
        };
    }
}
