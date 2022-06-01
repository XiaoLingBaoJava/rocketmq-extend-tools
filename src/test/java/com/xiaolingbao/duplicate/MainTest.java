package com.xiaolingbao.duplicate;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static com.xiaolingbao.duplicate.DuplicateConfig.USERKEY_AS_MSG_KEY;

/**
 * @author: xiaolingbao
 * @date: 2022/5/27 20:05
 * @description: 
 */
public class MainTest {

    public static void main(String[] args) throws MQClientException {
//        redisTest();
//        jdbcTest();
        redisAndJdbcTest();
    }

    @Test
    public static void redisTest() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("DUPLICATE-TEST-GROUP");
        consumer.subscribe("DUPLICATE-TEST-TOPIC", "*");
        // 指定NameServer
        consumer.setNamesrvAddr("192.168.20.100:9876");

        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        String appName = consumer.getConsumerGroup();
        StringRedisTemplate stringRedisTemplate = getRedisTemplate();
        DuplicateConfig duplicateConfig = DuplicateConfig.enableAvoidDuplicateConsumeConfig(appName, USERKEY_AS_MSG_KEY, stringRedisTemplate);
        AvoidDuplicateMsgListener msgListener = new DuplicateTest(duplicateConfig);
        consumer.registerMessageListener(msgListener);
        consumer.start();
        System.out.println("Consumer Started");
    }

    @Test
    public static void jdbcTest() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("DUPLICATE-TEST-GROUP");
        consumer.subscribe("DUPLICATE-TEST-TOPIC", "*");

        // 指定NameServer
        consumer.setNamesrvAddr("192.168.20.100:9876");

        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        String appName = consumer.getConsumerGroup();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        DuplicateConfig duplicateConfig = DuplicateConfig.enableAvoidDuplicateConsumeConfig(appName, USERKEY_AS_MSG_KEY, jdbcTemplate, "message_status");
        AvoidDuplicateMsgListener msgListener = new DuplicateTest(duplicateConfig);
        consumer.registerMessageListener(msgListener);
        consumer.start();
        System.out.println("Consumer Started");
    }

    @Test
    public static void redisAndJdbcTest() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("DUPLICATE-TEST-GROUP");
        consumer.subscribe("DUPLICATE-TEST-TOPIC", "*");

        // 指定NameServer
        consumer.setNamesrvAddr("192.168.20.100:9876");

        // 指定从第一条消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        String appName = consumer.getConsumerGroup();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        StringRedisTemplate redisTemplate = getRedisTemplate();
        DuplicateConfig duplicateConfig = DuplicateConfig.enableAvoidDuplicateConsumeConfig(appName, USERKEY_AS_MSG_KEY, redisTemplate, jdbcTemplate, "message_status");
        AvoidDuplicateMsgListener msgListener = new DuplicateTest(duplicateConfig);
        consumer.registerMessageListener(msgListener);
        consumer.start();
        System.out.println("Consumer Started");
    }

    public static StringRedisTemplate getRedisTemplate() {
        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration();
        redisConf.setPort(6379);
        redisConf.setHostName("127.0.0.1");
        JedisConnectionFactory factory = new JedisConnectionFactory(redisConf);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CREATE TABLE `message_status` (
     * `application_name` varchar(255) NOT NULL COMMENT '消费的应用名（可以用消费者组名称）',
     * `topic` varchar(255) NOT NULL COMMENT '消息来源的topic（不同topic消息不会认为重复）',
     * `tag` varchar(16) NOT NULL COMMENT '消息的tag（同一个topic不同的tag，就算去重键一样也不会认为重复），没有tag则存""字符串',
     * `msg_uniq_key` varchar(255) NOT NULL COMMENT '消息的唯一键（建议使用业务主键）',
     * `status` varchar(16) NOT NULL COMMENT '这条消息的消费状态',
     * `expire_time` bigint(20) NOT NULL COMMENT '这个去重记录的过期时间（时间戳）',
     * UNIQUE KEY `uniq_key` (`application_name`,`topic`,`tag`,`msg_uniq_key`) USING BTREE
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;
     */
    public static JdbcTemplate getJdbcTemplate() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        driverManagerDataSource.setUrl("jdbc:mysql://localhost:3306/mqtest?useUnicode=true&characterEncoding=UTF-8&useSSL=false");
        driverManagerDataSource.setUsername("root");
        driverManagerDataSource.setPassword("851310123");

        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(driverManagerDataSource);
        return jdbcTemplate;
    }

}
