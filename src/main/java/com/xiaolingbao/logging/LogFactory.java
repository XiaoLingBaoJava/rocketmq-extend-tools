package com.xiaolingbao.logging;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 14:50
 * @description: 
 */
public abstract class LogFactory {

    public static final String LOGGER_FILE = "logStoreInFile";

    public static final String LOGGER_CMD = "logShowInCMD";

    public static final String DEFAULT_LOGGER = LOGGER_CMD;

    private static String loggerType = null;

    private static ConcurrentHashMap<String, LogFactory> loggerFactoryCache = new ConcurrentHashMap<>();

    public static Log getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    public static Log getLogger(String name) {
        return getLoggerFactory().getLoggerInstance(name);
    }

    static {
        try {
            new InnerLogFactory();
        } catch (Throwable e) {

        }
    }

    private static LogFactory getLoggerFactory() {
        LogFactory logFactory = null;
        // 首先看是否设置了log的类别，设置了就采用设置的类别，否则使用默认的类别
        if (loggerType != null) {
            logFactory = loggerFactoryCache.get(loggerType);
        }
        if (logFactory == null) {
            logFactory = loggerFactoryCache.get(DEFAULT_LOGGER);
        }
        // 如果默认的类别Log为null，则取另一类别
        if (logFactory == null) {
            logFactory = loggerFactoryCache.get(LOGGER_FILE);
        }
        // 如果仍未取到，则抛出异常
        if (logFactory == null) {
            throw new RuntimeException("[RocketmqExtendTools] 日志启动失败,请查看日志设置");
        }
        return logFactory;
    }

    // 设置日志类别
    public static void setLogType(String type) {
        loggerType = type;
    }

    // 向HashMap中注册日志,若Map中已存在日志，则返回
    public void doRegister() {
        String logType = getLoggerType();
        if (loggerFactoryCache.get(logType) != null) {
            return;
        }
        loggerFactoryCache.put(logType, this);
    }

    public abstract void shutdown();

    public abstract Log getLoggerInstance(String name);

    public abstract String getLoggerType();
}
