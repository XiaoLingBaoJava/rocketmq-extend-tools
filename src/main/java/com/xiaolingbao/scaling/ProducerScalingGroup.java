package com.xiaolingbao.scaling;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 20:08
 * @description: 
 */
@Getter
@Setter
public class ProducerScalingGroup extends ScalingGroup<DefaultMQProducer> {

    private static final Log log = ClientLogger.getLog();
    
    private final List<DefaultMQProducer> producers = Collections.synchronizedList(new ArrayList<>());

    public ProducerScalingGroup(String name, int minInstanceNum, int maxInstanceNum, long frozenSeconds,
                                StartingTemplate<DefaultMQProducer> startingTemplate,
                                Function<List<DefaultMQProducer>, DefaultMQProducer> removeStrategy) {
        this.name = name;
        this.minInstanceNum = minInstanceNum;
        this.maxInstanceNum = maxInstanceNum;
        this.frozenSeconds = frozenSeconds;
        this.startingTemplate = startingTemplate;
        this.removeStrategy = removeStrategy;
    }

    @Override
    public boolean start() {
        ProducerStartingTemplate producerStartingTemplate;
        if (startingTemplate instanceof ProducerStartingTemplate) {
            producerStartingTemplate = (ProducerStartingTemplate) startingTemplate;
        } else {
            log.warn("ProducerScalingGroup的start方法参数不是ProducerStartingTemplate类型,扩容将不会进行, 参数类型: {}", startingTemplate.getClass());
            return false;
        }
        if (producers.size() >= maxInstanceNum) {
            log.warn("现有的伸缩组中的Producer数量已经达到设置的最大实例数,扩充producer失败");
            return false;
        }
        if (lastExecuteTime.plusSeconds(frozenSeconds).isAfter(LocalDateTime.now())) {
            log.warn("扩充producer尚处于冷却期中,扩充失败");
            return false;
        }
        DefaultMQProducer producer = producerStartingTemplate.start();
        if (producer != null) {
            log.info("producer扩充成功, producer: {}, scaleGroupName: {}", producer.toString(), name);
            lastExecuteTime = LocalDateTime.now();
            producers.add(producer);
            return true;
        }
        return false;
    }

    @Override
    public DefaultMQProducer remove() {
        if (producers.size() <= minInstanceNum) {
            log.warn("现有的伸缩组中的producer数量已经达到设置的最小实例数，移除producer失败");
            return null;
        }
        if (lastExecuteTime.plusSeconds(frozenSeconds).isAfter(LocalDateTime.now())) {
            log.warn("减少producer尚处于冷却期中,移除失败");
            return null;
        }
        DefaultMQProducer removeProducer = removeStrategy.apply(producers);
        removeProducer.shutdown();
        producers.remove(removeProducer);
        log.info("producer移除成功, producer: {}, scaleGroupName: {}", removeProducer.toString(), name);
        lastExecuteTime = LocalDateTime.now();
        return removeProducer;
    }
}
