package com.xiaolingbao.duplicate.persistence;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.concurrent.TimeUnit;

/**
 * @author: xiaolingbao
 * @date: 2022/5/22 11:19
 * @description: 
 */
public class RedisPersistence implements Persist {

    private static final Log log = ClientLogger.getLog();

    private final StringRedisTemplate redisTemplate;

    public RedisPersistence(StringRedisTemplate redisTemplate) {
        if (redisTemplate == null) {
            log.error("redis template为空", new NullPointerException());
            throw new NullPointerException("redis template为空");
        }
        this.redisTemplate = redisTemplate;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/22 14:55
     * @param persistenceElement
     * @param duplicateProcessingExpireMilliSeconds
     * @return set redis key成功返回true,否则返回false
     * @description: 向redis中加入redisKey
     */
    @Override
    public boolean doPersistence(PersistenceElement persistenceElement, long duplicateProcessingExpireMilliSeconds) {
        String redisKey = buildDuplicateMsgRedisKey(persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(), persistenceElement.getMsgUniqKey());

        // set redisKey
        Boolean isSuccess = redisTemplate.execute((RedisCallback<Boolean>) redisConnection ->
            redisConnection.set(redisKey.getBytes(), (CONSUME_STATUS_CONSUMING).getBytes(), Expiration.milliseconds(duplicateProcessingExpireMilliSeconds), RedisStringCommands.SetOption.SET_IF_ABSENT));


        if (isSuccess == null) {
            return false;
        }

        return isSuccess;
    }

    @Override
    public void delete(PersistenceElement persistenceElement) {
        String redisKey = buildDuplicateMsgRedisKey(persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(), persistenceElement.getMsgUniqKey());
        redisTemplate.delete(redisKey);
    }

    @Override
    public void markConsumed(PersistenceElement persistenceElement, long duplicateRecordReserveMinutes) {
        String redisKey = buildDuplicateMsgRedisKey(persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(), persistenceElement.getMsgUniqKey());
        redisTemplate.opsForValue().set(redisKey, CONSUME_STATUS_CONSUMED, duplicateRecordReserveMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String get(PersistenceElement persistenceElement) {
        String redisKey = buildDuplicateMsgRedisKey(persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(), persistenceElement.getMsgUniqKey());
        return redisTemplate.opsForValue().get(redisKey);
    }



    /**
     * @author: xiaolingbao
     * @date: 2022/5/22 14:33
     * @param applicationName
     * @param tag
     * @param msgUniqKey
     * @return java.lang.String
     * @description: 创建消息在redis中的标识
     */
    private String buildDuplicateMsgRedisKey(String applicationName, String topic, String tag, String msgUniqKey) {
        if (StringUtils.isEmpty(msgUniqKey)) {
            log.error("msgUniqKey为空,无法创建消息在redis中的key");
            return null;
        } else {
            // MSGDUPLICATEKEY:APPLICATIONNAME:TOPIC:TAG:MSGUNIQKEY
            String prefix = StringUtils.join("MSGDUPLICATEKEY:", applicationName, ":", topic, (StringUtils.isNoneEmpty(tag)) ? ":" + tag : "");
            return StringUtils.join(prefix, ":", msgUniqKey);
        }
    }

    @Override
    public String persistenceElementToString(PersistenceElement persistenceElement) {
        return buildDuplicateMsgRedisKey(persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(), persistenceElement.getMsgUniqKey());
    }
}
