# RocketmqExtendTools

由于RocketMQ官方提供的API和RocketmqConsole只提供了一些基本功能，在实际开发中往往需要开发者基于基本API进行扩展，该项目的目的是基于RocketMQ扩展其功能，对于官方未直接实现的功能提供一些易于使用的API，供大家使用或借鉴，降低学习成本，提高效率

目前已实现的功能有：

1、消息幂等去重解决方案，防止消息被重复消费

2、Producer和Consumer的弹性伸缩，基于资源使用情况自动伸缩生产者消费者

3、实现了一些常用功能：获取消息在队列中的逻辑位置、获取生产和消费的tps、获取队列及其所属消费者的对应关系等



## 消息幂等去重

消息幂等去重的实现在duplicate包中，支持使用redis或MySQL做幂等表，也支持同时使用redis和MySQL做双重检查



使用消息幂等去重时，Consumer的启动配置和平常相比区别不大，唯一区别在于需要用户自定义一个类，让其继承AvoidDuplicateMsgListener，并将其注册为consumer的MessageListener



### 使用说明

1、创建自定义类，使其继承AvoidDuplicateMsgListener，注意它的构造函数的参数为DuplicateConfig，构造时调用父类的构造函数，该类重写的doHandleMsg方法，为实际消费消息的逻辑

```java
public class DuplicateTest extends AvoidDuplicateMsgListener {

    // 构造函数必须这么写，调用父类的构造函数，参数为DuplicateConfig
    public DuplicateTest(DuplicateConfig duplicateConfig) {
        super(duplicateConfig);
    }

    // 重写的该方法为消费消息的逻辑
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
```

2、根据使用redis或/和MySQL，使用消息的uniqKey还是业务key（业务key是指在发送消息时调用setKeys方法设置的key）进行去重，调用不同的enableAvoidDuplicateConsumeConfig方法获取DuplicateConfig

```java
// 使用redis和MySQL,指定MySQL表名,applicationName表示针对什么应用做去重，相同的消息在不同应用的去重是隔离处理的,msgKeyStrategy可以为USERKEY_AS_MSG_KEY也可以为UNIQKEY_AS_MSG_KEY
public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate, String tableName) {
    return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, redisTemplate, jdbcTemplate, tableName);
}

// 使用redis
public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, StringRedisTemplate redisTemplate) {
    return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, redisTemplate);
}

// 使用MySQL
public static DuplicateConfig enableAvoidDuplicateConsumeConfig(String applicationName, int msgKeyStrategy, JdbcTemplate jdbcTemplate, String tableName) {
    return new DuplicateConfig(applicationName, AVOID_DUPLICATE_ENABLE, msgKeyStrategy, jdbcTemplate, tableName);
}
```

注意MySQL幂等表的列是固定的，表名可以自定义，建表语句为：

```sql
CREATE TABLE `xxxxx` (
`application_name` varchar(255) NOT NULL COMMENT '消费的应用名（可以用消费者组名称）',
`topic` varchar(255) NOT NULL COMMENT '消息来源的topic（不同topic消息不会认为重复）',
`tag` varchar(16) NOT NULL COMMENT '消息的tag（同一个topic不同的tag，就算去重键一样也不会认为重复），没有tag则存""字符串',
`msg_uniq_key` varchar(255) NOT NULL COMMENT '消息的唯一键（建议使用业务主键）',
`status` varchar(16) NOT NULL COMMENT '这条消息的消费状态',
`expire_time` bigint(20) NOT NULL COMMENT '这个去重记录的过期时间（时间戳）',
UNIQUE KEY `uniq_key` (`application_name`,`topic`,`tag`,`msg_uniq_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;
```

3、new自定义类，将其注册为consumer的MessageListener，幂等去重即可生效，这里以使用redis为例，其他情况代码可以看test包下的duplicate包的MainTest类

```java
@Test
public static void redisTest() throws MQClientException {
    DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("DUPLICATE-TEST-GROUP");
    consumer.subscribe("DUPLICATE-TEST-TOPIC", "*");
    // 指定NameServer,需要用户指定NameServer地址
    consumer.setNamesrvAddr("192.168.20.100:9876");

    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

    String appName = consumer.getConsumerGroup();
    // 需要用户提供StringRedisTemplate
    StringRedisTemplate stringRedisTemplate = xxx;
    // 调用DuplicateConfig的enableAvoidDuplicateConsumeConfig方法创建DuplicateConfig
    DuplicateConfig duplicateConfig = DuplicateConfig.enableAvoidDuplicateConsumeConfig(appName, USERKEY_AS_MSG_KEY, stringRedisTemplate);
    // 创建用户自定义的Listener类
    AvoidDuplicateMsgListener msgListener = new DuplicateTest(duplicateConfig);
    // 将Listener注册到consumer，幂等去重即生效
    consumer.registerMessageListener(msgListener);
    consumer.start();
}
```



## Producer和Consumer的弹性伸缩

Producer和Consumer的弹性伸缩的实现在scaling包中，支持选择对cpu、内存、produce tps、consume tps指标进行监控，并依据此来弹性扩容或缩减Producer或Consumer的数量

### 使用说明

#### 消费者弹性伸缩

1、创建消费者启动模板，需要用户提供消费者组名称、NameServer地址、ConsumeFromWhere、订阅的topic和tag、消费模式（集群或广播）、MessageListenerConcurrently

```java
Map<String, String> topicAndTag = new HashMap<>();
topicAndTag.put("SCALE", "*");
// 参数分别为：消费者组名称、NameServer地址、ConsumeFromWhere、订阅的topic和tag（用Map保存）、消费模式（集群或广播）、MessageListenerConcurrently
ConsumerStartingTemplate consumerStartingTemplate = new ConsumerStartingTemplate("SCALE_TEST_CONSUMER_GROUP", "192.168.20.100:9876", ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET,
        topicAndTag, MessageModel.CLUSTERING, new MessageListenerConcurrently() {
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        // 逐条消费消息
        for (MessageExt msg : msgs) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 返回消费状态:消费成功
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
});
```

2、创建消费者伸缩组，需要用户提供伸缩组名称、最少消费者实例数、最多消费者实例数、冷却时间（即连续两次伸缩的最短时间间隔）、消费者启动模板、收缩策略（去掉最晚或最早创建的消费者，ConsumerRemoveStrategy提供了getOldestRemoveStrategy()和getNewestRemoveStrategy()方法）

```java
// 参数分别为伸缩组名称、最少消费者实例数、最多消费者实例数、冷却时间（即连续两次伸缩的最短时间间隔，单位为秒）、消费者启动模板、收缩策略（去掉最早或最晚创建的消费者）
ConsumerScalingGroup consumerScalingGroup = new ConsumerScalingGroup("CONSUMER-SCALE", 0, 3, 5,
                                                consumerStartingTemplate, new ConsumerRemoveStrategy().getOldestRemoveStrategy());
```

3、创建监控器类（有四种监控器：CpuMonitorRunnable、MemoryMonitorRunnable、ProduceTpsMonitor、ConsumeTpsMonitor，分别监控cpu、内存、生产tps、消费tps），需要用户提供监控器名、伸缩组名、伸缩组、定时执行的cron表达式、统计持续时间（单位：秒）、统计方式（最大、最小、平均值）、扩容阈值、缩容阈值、连续达到阈值多少次触发伸缩，如果是生产和消费tps监控，还需提供brokerAddr

```java
// 参数为监控器名、伸缩组名、伸缩组、定时执行的cron表达式、统计持续时间（单位：秒）、统计方式（最大、最小、平均值）、扩容阈值、缩容阈值、连续达到阈值多少次触发伸缩
CpuMonitorRunnable cpuMonitorRunnable = new CpuMonitorRunnable("CPU-CONSUMER-MONITER", "CONSUMER-SCALE", consumerScalingGroup,
                                            "0/5 * * * * ?", 3, MonitorMethod.AVG, 80, 20, 3);

// 如果是生产和消费tps监控，还需提供brokerAddr
ProduceTpsMonitor produceTpsMonitor = new ProduceTpsMonitor("PRODUCE_TPS_MONITOR", "PRODUCER_SCALING_GROUP", producerScalingGroup,
                "0/5 * * * * ?", 3, MonitorMethod.AVG, 0, -1, 3, "192.168.20.100:10911");
```

4、创建MonitorManager，并调用其startMonitor方法启动第3步创建的monitor，弹性伸缩即启动

```javascript
MonitorManager monitorManager = new MonitorManager();
monitorManager.startMonitor(cpuMonitorRunnable);
```

5、消费者弹性伸缩开启的全部代码如下，也可查看test包下的scaling包的ConsumerScalingTest类

```java
    @Test
    public void testScalingConsumer() {
        Map<String, String> topicAndTag = new HashMap<>();
        topicAndTag.put("SCALE", "*");
        // 创建消费者启动模板
        ConsumerStartingTemplate consumerStartingTemplate = new ConsumerStartingTemplate("SCALE_TEST_CONSUMER_GROUP", "192.168.20.100:9876", ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET,
                topicAndTag, MessageModel.CLUSTERING, new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                // 逐条消费消息
                for (MessageExt msg : msgs) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 返回消费状态:消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 创建消费者伸缩组
        ConsumerScalingGroup consumerScalingGroup = new ConsumerScalingGroup("CONSUMER-SCALE", 0, 3, 5,
                                                        consumerStartingTemplate, new ConsumerRemoveStrategy().getOldestRemoveStrategy());
        // 创建资源监控器
        CpuMonitorRunnable cpuMonitorRunnable = new CpuMonitorRunnable("CPU-CONSUMER-MONITER", "CONSUMER-SCALE", consumerScalingGroup,
                                                    "0/5 * * * * ?", 3, MonitorMethod.AVG, 80, 20, 3);
        // 这里只是为了在windows系统下进行测试，假设cpu利用率一直为90%
        cpuMonitorRunnable.setOs("windows");
        cpuMonitorRunnable.setCmd("echo 90");
//        cpuMonitorRunnable.setCmd("echo 10");
        // 创建MonitorManager，并启动monitor
        MonitorManager monitorManager = new MonitorManager();
        monitorManager.startMonitor(cpuMonitorRunnable);

        // while true是避免程序结束，为了打印出日志信息
        while (true) {

        }
    }
```

#### 生产者弹性伸缩

1、创建生产者启动模板，需要用户提供生产者组名、NameServer地址、发送失败后的重试次数、一个topic对应多少个Queue

```java
// 参数为生产者组名、NameServer地址、发送失败后的重试次数、一个topic对应多少个Queue
ProducerStartingTemplate producerStartingTemplate = new ProducerStartingTemplate("SCALE_PRODUCER_GROUP",
        "192.168.20.100:9876", 0, 2);
```

2、创建生产者伸缩组，需要用户提供伸缩组名、最小生产者实例数、最大生产者实例数、冷却时间、生产者启动模板、收缩策略（去掉最晚或最早创建的生产者，ProducerRemoveStrategy提供了getOldestRemoveStrategy()和getNewestRemoveStrategy()方法）

```java
// 参数为伸缩组名、最小生产者实例数、最大生产者实例数、冷却时间、生产者启动模板、收缩策略
ProducerScalingGroup producerScalingGroup = new ProducerScalingGroup("PRODUCER_SCALING_GROUP", 0, 3, 5, producerStartingTemplate, new ProducerRemoveStrategy().getOldestRemoveStrategy());
```

3、创建监控器类，这里以生产tps监控器为例，需要用户提供监控器名、伸缩组名、伸缩组、定时执行的cron表达式、统计持续时间（单位：秒）、统计方式（最大、最小、平均值）、扩容阈值、缩容阈值、连续达到阈值多少次触发伸缩，brokerAddr

```java
// 参数为监控器名、伸缩组名、伸缩组、定时执行的cron表达式、统计持续时间（单位：秒）、统计方式（最大、最小、平均值）、扩容阈值、缩容阈值、连续达到阈值多少次触发伸缩，brokerAddr
ProduceTpsMonitor produceTpsMonitor = new ProduceTpsMonitor("PRODUCE_TPS_MONITOR", "PRODUCER_SCALING_GROUP", producerScalingGroup,
        "0/5 * * * * ?", 3, MonitorMethod.AVG, 0, -1, 3, "192.168.20.100:10911");
```

4、创建MonitorManager并启动Monitor,弹性伸缩即启动

```java
MonitorManager monitorManager = new MonitorManager();
monitorManager.startMonitor(produceTpsMonitor);
```

5、生产者弹性伸缩开启的全部代码如下，也可查看test包下的scaling包的ProducerScalingTest类

```java
@Test
public void testScalingProducer() {
    // 创建生产者启动模板
    ProducerStartingTemplate producerStartingTemplate = new ProducerStartingTemplate("SCALE_PRODUCER_GROUP",
            "192.168.20.100:9876", 0, 2);
    // 创建生产者伸缩组
    ProducerScalingGroup producerScalingGroup = new ProducerScalingGroup("PRODUCER_SCALING_GROUP", 0, 3, 5, producerStartingTemplate, new ProducerRemoveStrategy().getOldestRemoveStrategy());
    // 创建资源监控器
    ProduceTpsMonitor produceTpsMonitor = new ProduceTpsMonitor("PRODUCE_TPS_MONITOR", "PRODUCER_SCALING_GROUP", producerScalingGroup,
            "0/5 * * * * ?", 3, MonitorMethod.AVG, 0, -1, 3, "192.168.20.100:10911");
    // 创建MonitorManager并启动Monitor
    MonitorManager monitorManager = new MonitorManager();
    monitorManager.startMonitor(produceTpsMonitor);

    // while true是避免程序结束，为了打印出日志信息
    while (true) {

    }
}
```



## 其他功能

其他功能的实现在service包，入口为impl包，提供了BrokerServiceImpl、MessageServiceImpl、QueueServiceImpl、TopicServiceImpl，以下列出3个功能为例子

### 获取消息在队列中的逻辑位置

```java
// 创建MessageServiceImpl
MessageServiceImpl messageService = new MessageServiceImpl();
// 在生产者发送成功消息后，将SendResult作为参数，调用getMessageLogicPositionInQueueAfterSend方法可以得到消息在队列中的逻辑位置
Long logicPos = messageService.getMessageLogicPositionInQueueAfterSend(sendResult);
```

### 获取生产和消费的tps

```java
// 创建BrokerServiceImpl
BrokerServiceImpl brokerServiceImpl = new BrokerServiceImpl();
// 调用getProduceAndConsumeTpsByBrokerAddr(String brokerAddr)方法获取生产和消费的tps,返回值为一个Map,对应key为"produceTps"的value为生产tps，对应key为"consumeTps"的value为消费tps
Map<String, Long> map = brokerServiceImpl.getProduceAndConsumeTpsByBrokerAddr("192.168.20.100:10911");
Long produceTps = map.get("produceTps");
Long consumeTps = map.get("consumeTps");
```

### 获取队列及其所属消费者的对应关系

```java
// 创建QueueServiceImpl
QueueServiceImpl queueServiceImpl = new QueueServiceImpl();
// 调用getQueueConsumerRelationByConsumerGroup(String consumerGroupName)方法通过消费者组名获取队列及其所属的消费者的对应关系，返回值为Map<MessageQueue, String>
queueServiceImpl.getQueueConsumerRelationByConsumerGroup(xxx);
```

