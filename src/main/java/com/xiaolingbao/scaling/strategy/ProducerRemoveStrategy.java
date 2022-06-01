package com.xiaolingbao.scaling.strategy;

import org.apache.rocketmq.client.producer.DefaultMQProducer;

import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 20:15
 * @description: 
 */
public class ProducerRemoveStrategy implements RemoveStrategy<DefaultMQProducer> {

    @Override
    public Function<List<DefaultMQProducer>, DefaultMQProducer> getNewestRemoveStrategy() {
        return new Function<List<DefaultMQProducer>, DefaultMQProducer>() {
            @Override
            public DefaultMQProducer apply(List<DefaultMQProducer> defaultMQProducers) {
                return defaultMQProducers.get(0);
            }
        };
    }

    @Override
    public Function<List<DefaultMQProducer>, DefaultMQProducer> getOldestRemoveStrategy() {
        return new Function<List<DefaultMQProducer>, DefaultMQProducer>() {
            @Override
            public DefaultMQProducer apply(List<DefaultMQProducer> defaultMQProducers) {
                return defaultMQProducers.get(defaultMQProducers.size() - 1);
            }
        };
    }
}
