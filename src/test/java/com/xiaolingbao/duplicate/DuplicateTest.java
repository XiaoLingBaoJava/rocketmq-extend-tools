package com.xiaolingbao.duplicate;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static com.xiaolingbao.duplicate.DuplicateConfig.UNIQKEY_AS_MSG_KEY;
import static com.xiaolingbao.duplicate.DuplicateConfig.USERKEY_AS_MSG_KEY;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 16:36
 * @description: 
 */
public class DuplicateTest extends AvoidDuplicateMsgListener {

    private static final Log log = ClientLogger.getLog();

    public DuplicateTest(DuplicateConfig duplicateConfig) {
        super(duplicateConfig);
    }

    @Override
    protected boolean doHandleMsg(MessageExt messageExt) {
        switch (messageExt.getTopic()) {
            case "DUPLICATE-TEST-TOPIC": {
                log.info("消费中..., messageId: {}", messageExt.getMsgId());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        return true;
    }
}
