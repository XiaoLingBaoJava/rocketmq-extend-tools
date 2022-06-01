package com.xiaolingbao.duplicate;

import com.xiaolingbao.duplicate.persistence.JDBCPersistence;
import com.xiaolingbao.duplicate.persistence.RedisPersistence;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/21 20:36
 * @description: 
 */
@Getter
@ToString
public class DuplicateConfig {

    private static final Log log = ClientLogger.getLog();

    // 不启用去重
    public static final int AVOID_DUPLICATE_DISABLE = 0;

    // 开启去重，若有重复的消息正在被消费，则之后再重试（重试是因为当前正在被消费的消息可能消费失败，此时需要重复消息稍后被消费）
    public static final int AVOID_DUPLICATE_ENABLE = 1;

    // 使用uniqKey作为幂等令牌
    public static final int UNIQKEY_AS_MSG_KEY = 2;

    // 使用业务key作为幂等令牌
    public static final int USERKEY_AS_MSG_KEY = 3;

    // 用以标记去重的时候是哪个应用消费的，同一个应用才需要去重,相同的消息在不同应用的去重是隔离处理的
    private String applicationName;

    private String tableName;

    private RedisPersistence redisPersistence = null;

    private JDBCPersistence jdbcPersistence = null;

    // 去重策略，默认不去重
    private int duplicateStrategy = AVOID_DUPLICATE_DISABLE;

    // CONSUMING（消费中）这个状态保存多久，默认为一分钟，即一分钟内的重复消息都会串行处理（等待前一个消息消费成功/失败）
    private long duplicateProcessingExpireMilliSeconds = 60 * 1000;

    // CONSUMED（消费成功）这个状态保存多久，默认为一天，即已成功消费的消息对应的重复消息在一天内不会被消费
    private long duplicateRecordReserverMinutes = 24 * 60;

    // 幂等令牌选取策略，默认使用uniqKey
    private int msgKeyStrategy = UNIQKEY_AS_MSG_KEY;

    public String getMsgDuplicateKey(MessageExt messageExt, int msgKeyStrategy) {
        String uniqKey;
        if (msgKeyStrategy == UNIQKEY_AS_MSG_KEY) {
            uniqKey = MessageClientIDSetter.getUniqID(messageExt);
        } else if (msgKeyStrategy == USERKEY_AS_MSG_KEY) {
            uniqKey = messageExt.getKeys();
        } else {
            uniqKey = messageExt.getMsgId();
        }
        if (uniqKey == null) {
            log.warn("getMsgDuplicateKey方法返回的幂等令牌为空, messageExt: {}, msgKeyStrategy: {}", messageExt, msgKeyStrategy);
            return messageExt.getMsgId();
        }
        return uniqKey;
    }

    private DuplicateConfig(String applicationName, int duplicateStrategy, int msgKeyStrategy, StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate, String tableName) {
        if (redisTemplate != null) {
            this.redisPersistence = new RedisPersistence(redisTemplate);
        }
        if (jdbcTemplate != null) {
            if (StringUtils.isEmpty(tableName)) {
                log.error("未设置幂等表的名称tableName, 需使用setTableName方法");
            } else {
                this.tableName = tableName;
                this.jdbcPersistence = new JDBCPersistence(jdbcTemplate, tableName);
            }
        }
        this.duplicateStrategy = duplicateStrategy;
        this.msgKeyStrategy = msgKeyStrategy;
        this.applicationName = applicationName;
    }

    private DuplicateConfig(String applicationName, int duplicateStrategy, int msgKeyStrategy, StringRedisTemplate redisTemplate) {
        this(applicationName, duplicateStrategy, msgKeyStrategy, redisTemplate, null, null);
    }

    private DuplicateConfig(String applicationName, int duplicateStrategy, int msgKeyStrategy, JdbcTemplate jdbcTemplate, String tableName) {
        this(applicationName, duplicateStrategy, msgKeyStrategy, null, jdbcTemplate, tableName);
    }

    private DuplicateConfig(String applicationName) {
        this.duplicateStrategy = AVOID_DUPLICATE_DISABLE;
        this.applicationName = applicationName;
    }

    public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate, String tableName) {
        return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, redisTemplate, jdbcTemplate, tableName);
    }

    public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, StringRedisTemplate redisTemplate) {
        return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, redisTemplate);
    }

    public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, JdbcTemplate jdbcTemplate, String tableName) {
        return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, jdbcTemplate, tableName);
    }

    public static DuplicateConfig disableAvoidDuplicateConsumeConfig(String applicationName) {
        return new DuplicateConfig(applicationName);
    }


    public void setDuplicateProcessingExpireMilliSeconds(long duplicateProcessingExpireMilliSeconds) {
        this.duplicateProcessingExpireMilliSeconds = duplicateProcessingExpireMilliSeconds;
    }

    public void setDuplicateRecordReserverMinutes(long duplicateRecordReserverMinutes) {
        this.duplicateRecordReserverMinutes = duplicateRecordReserverMinutes;
    }
}
