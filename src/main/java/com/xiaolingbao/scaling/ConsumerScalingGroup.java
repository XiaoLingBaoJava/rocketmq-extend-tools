package com.xiaolingbao.scaling;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.list.SynchronizedList;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/28 17:02
 * @description: 
 */
@Getter
@Setter
public class ConsumerScalingGroup extends ScalingGroup<DefaultMQPushConsumer> {

    private static final Log log = ClientLogger.getLog();

    private final List<DefaultMQPushConsumer> consumers = Collections.synchronizedList(new ArrayList<>());

    public ConsumerScalingGroup(String name, int minInstanceNum, int maxInstanceNum, long frozenSeconds,
                                StartingTemplate<DefaultMQPushConsumer> startingTemplate,
                                Function<List<DefaultMQPushConsumer>, DefaultMQPushConsumer> removeStrategy) {
        this.name = name;
        this.minInstanceNum = minInstanceNum;
        this.maxInstanceNum = maxInstanceNum;
        this.frozenSeconds = frozenSeconds;
        this.startingTemplate = startingTemplate;
        this.removeStrategy = removeStrategy;
    }

    @Override
    public boolean start() {
        ConsumerStartingTemplate consumerStartingTemplate;
        if (startingTemplate instanceof ConsumerStartingTemplate) {
            consumerStartingTemplate = (ConsumerStartingTemplate) startingTemplate;
        } else {
            log.warn("ConsumerScalingGroup的start方法参数不是ConsumerStartingTemplate类型,扩容将不会进行, 参数类型: {}", startingTemplate.getClass());
            return false;
        }
        if (consumers.size() >= maxInstanceNum) {
            log.warn("现有的伸缩组中的Consumer数量已经达到设置的最大实例数,扩充consumer失败");
            return false;
        }
        if (lastExecuteTime.plusSeconds(frozenSeconds).isAfter(LocalDateTime.now())) {
            log.warn("扩充consumer尚处于冷却期中,扩充失败");
            return false;
        }
        DefaultMQPushConsumer consumer = consumerStartingTemplate.start();
        if (consumer != null) {
            log.info("Consumer扩充成功, consumer: {}, scaleGroupName: {}", consumer.toString(), name);
            lastExecuteTime = LocalDateTime.now();
            consumers.add(consumer);
            return true;
        }
        return false;
    }


    @Override
    public DefaultMQPushConsumer remove() {
        if (consumers.size() <= minInstanceNum) {
            log.warn("现有的伸缩组中的Consumer数量已经达到设置的最小实例数，移除consumer失败");
            return null;
        }
        if (lastExecuteTime.plusSeconds(frozenSeconds).isAfter(LocalDateTime.now())) {
            log.warn("减少consumer尚处于冷却期中,移除失败");
            return null;
        }
        DefaultMQPushConsumer removeConsumer = removeStrategy.apply(consumers);
        removeConsumer.shutdown();
        consumers.remove(removeConsumer);
        log.info("Consumer移除成功, consumer: {}, scaleGroupName: {}", removeConsumer.toString(), name);
        lastExecuteTime = LocalDateTime.now();
        return removeConsumer;
    }
}
