package com.xiaolingbao.service.client;

import com.google.common.collect.Lists;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExtImpl;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.joor.Reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author: xiaolingbao
 * @date: 2022/5/16 20:18
 * @description: 
 */
public class MQAdminInstance {

    private static final Log log = ClientLogger.getLog();

    private static final ThreadLocal<DefaultMQAdminExt> MQ_ADMIN_EXT_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Integer> INIT_COUNTER = new ThreadLocal<>();

    public static MQAdminExt threadLocalMQAdminExt() {
        DefaultMQAdminExt defaultMQAdminExt = MQ_ADMIN_EXT_THREAD_LOCAL.get();
        if (defaultMQAdminExt == null) {
            log.error("defaultMQAdminExt应在get前被初始化", new IllegalStateException());
        }
        return defaultMQAdminExt;
    }

    public static RemotingClient threadLocalRemotingClient() {
        MQClientInstance mqClientInstance = threadLocalMqClientInstance();
        MQClientAPIImpl mQClientAPIImpl = Reflect.on(mqClientInstance).get("mQClientAPIImpl");
        return Reflect.on(mQClientAPIImpl).get("remotingClient");
    }




    public static MQClientInstance threadLocalMqClientInstance() {
        DefaultMQAdminExtImpl defaultMQAdminExtImpl = Reflect.on(MQAdminInstance.threadLocalMQAdminExt()).get("defaultMQAdminExtImpl");
        return Reflect.on(defaultMQAdminExtImpl).get("mqClientInstance");
    }

    public static void initMQAdminInstance(long timeoutMillis) throws MQClientException {
        Integer nowCount = INIT_COUNTER.get();
        if (nowCount == null) {
            DefaultMQAdminExt defaultMQAdminExt;
            if (timeoutMillis > 0) {
                defaultMQAdminExt = new DefaultMQAdminExt(timeoutMillis);
            } else {
                defaultMQAdminExt = new DefaultMQAdminExt();
            }
            defaultMQAdminExt.setInstanceName(Long.toString(System.currentTimeMillis()));
            try {
                defaultMQAdminExt.start();
            } catch (MQClientException e) {
                defaultMQAdminExt.setAdminExtGroup(Long.toString(System.currentTimeMillis()));
                defaultMQAdminExt.start();
            }
            MQ_ADMIN_EXT_THREAD_LOCAL.set(defaultMQAdminExt);
            // 这一步很关键，需要手动更新broker address list
            ((NettyRemotingClient)(threadLocalRemotingClient())).updateNameServerAddressList(Lists.newArrayList("192.168.20.100:9876"));
            INIT_COUNTER.set(1);
        } else {
            INIT_COUNTER.set(nowCount + 1);
        }
    }

    public static void destroyMQAdminInstance() {
        Integer nowCount = INIT_COUNTER.get() - 1;
        if (nowCount > 0) {
            INIT_COUNTER.set(nowCount);
            return;
        }
        MQAdminExt mqAdminExt = MQ_ADMIN_EXT_THREAD_LOCAL.get();
        if (mqAdminExt != null) {
            mqAdminExt.shutdown();
            MQ_ADMIN_EXT_THREAD_LOCAL.remove();
            INIT_COUNTER.remove();
        }
    }

}
