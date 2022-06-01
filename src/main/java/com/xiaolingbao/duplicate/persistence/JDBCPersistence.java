package com.xiaolingbao.duplicate.persistence;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/22 11:19
 * @description: 
 */
public class JDBCPersistence implements Persist {

    private Log log = ClientLogger.getLog();

    private final JdbcTemplate jdbcTemplate;

    private final String tableName;

    public JDBCPersistence(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public boolean doPersistence(PersistenceElement persistenceElement, long duplicateProcessingExpireMilliSeconds) {
        long expireTime = System.currentTimeMillis() + duplicateProcessingExpireMilliSeconds;
        try {
            jdbcTemplate.update(StringUtils.join("INSERT INTO ", tableName, "(application_name, topic, tag, msg_uniq_key, status, expire_time) ", "values (?, ?, ?, ?, ?, ?)"),
                                persistenceElement.getApplication(), persistenceElement.getTopic(),
                                persistenceElement.getTag(), persistenceElement.getMsgUniqKey(),
                                CONSUME_STATUS_CONSUMING, expireTime);
        } catch (DuplicateKeyException e) {
            log.warn("找到了重复的consuming/consumed记录, 持久化消息消费状态失败, {}", persistenceElement);
            /**
             * 由于mysql不支持消息过期，出现重复主键的情况下，有可能是过期的一些记录，这里动态的删除这些记录后重试
             */
            int num = delete(persistenceElement, true);
            if (num > 0) {
                // 若删除了过期消息
                log.info("删除了{}条过期消费状态记录, 现在重新尝试持久化消息消费状态", num);
                return doPersistence(persistenceElement, duplicateProcessingExpireMilliSeconds);
            } else {
                // 持久化失败, 因为数据库中存在尚未过期的消费记录
                return false;
            }
        } catch (Exception e) {
            log.error("在jdbc insert时出现未知错误", e);
            return false;
        }

        // 插入成功返回true
        return true;
    }

    private int delete(PersistenceElement persistenceElement, boolean onlyDeleteExpired) {
        if (onlyDeleteExpired) {
            return jdbcTemplate.update(StringUtils.join("DELETE FROM ", tableName, " WHERE application_name = ? AND topic = ? AND tag = ? AND msg_uniq_key = ? AND expire_time < ?"),
                                        persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(),
                                        persistenceElement.getMsgUniqKey(), System.currentTimeMillis());
        } else {
            return jdbcTemplate.update(StringUtils.join("DELETE FROM ", tableName, " WHERE application_name = ? AND topic = ? AND tag = ? AND msg_uniq_key = ?"),
                    persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(),
                    persistenceElement.getMsgUniqKey());
        }
    }

    @Override
    public void delete(PersistenceElement persistenceElement) {
        delete(persistenceElement, false);
    }

    @Override
    public void markConsumed(PersistenceElement persistenceElement, long duplicateRecordReserveMinutes) {
        long expireTime = System.currentTimeMillis() + duplicateRecordReserveMinutes * 60 * 1000;
        jdbcTemplate.update(StringUtils.join("UPDATE ", tableName, " SET status = ?, expire_time = ? WHERE application_name = ? AND topic = ? AND tag = ? AND msg_uniq_key = ?"),
                            CONSUME_STATUS_CONSUMED, expireTime, persistenceElement.getApplication(),
                            persistenceElement.getTopic(), persistenceElement.getTag(),
                            persistenceElement.getMsgUniqKey());
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/24 10:35
     * @param persistenceElement
     * @return java.lang.String
     * @description: 获取消费状态
     */
    @Override
    public String get(PersistenceElement persistenceElement) {
        Map<String, Object> res = jdbcTemplate.queryForMap(StringUtils.join("SELECT status FROM ", tableName, " where application_name = ? AND topic = ? AND tag = ? AND msg_uniq_key = ? AND expire_time > ?"),
                                                            persistenceElement.getApplication(), persistenceElement.getTopic(), persistenceElement.getTag(),
                                                            persistenceElement.getMsgUniqKey(), System.currentTimeMillis());
        return (String)res.get("status");
    }

}
