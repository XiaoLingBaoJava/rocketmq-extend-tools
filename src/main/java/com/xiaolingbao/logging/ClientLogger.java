package com.xiaolingbao.logging;

import com.xiaolingbao.logging.inner.*;

/**
 * @author: xiaolingbao
 * @date: 2022/5/14 11:21
 * @description: ClientLogger是最外层的日志接口，用户通过该类创建logger并进行日志操作
 */
public class ClientLogger {

    private static final Log LOGGER;

    private static Appender appenderProxy = new AppenderProxy();

    static {
        if (ClientLoggerConfig.isUseFileLog()) {
            LogFactory.setLogType(InnerLogFactory.LOGGER_FILE);
        } else {
            LogFactory.setLogType(InnerLogFactory.LOGGER_CMD);
        }
        LOGGER = createLogger(ClientLoggerConfig.getLoggerName());
        Logger.getRootLogger().addAppender(appenderProxy);
    }

    private static synchronized Appender createClientAppender() {


        Layout layout = LoggingBuilder.newLayoutBuilder().withDefaultLayout().build();
        if (ClientLoggerConfig.isUseFileLog()) {
            String realLogFileName = ClientLoggerConfig.getLogRoot() + "/" + ClientLoggerConfig.getClientLogFileName();
            Appender rocketmqExtendToolAppender = LoggingBuilder.newAppenderBuilder()
                    .withRollingFileAppender(realLogFileName, ClientLoggerConfig.getLogFileSize(), ClientLoggerConfig.getLogMaxFileIndex())
                    .withAsync(false, ClientLoggerConfig.getLogAsyncQueueSize())
                    .withName(ClientLoggerConfig.getAppenderName())
                    .withLayout(layout)
                    .build();
            return rocketmqExtendToolAppender;
        } else {
            Appender rocketmqExtendToolAppender = LoggingBuilder.newAppenderBuilder()
                    .withConsoleAppender(LoggingBuilder.SYSTEM_OUT)
                    .withAsync(false, ClientLoggerConfig.getLogAsyncQueueSize())
                    .withName(ClientLoggerConfig.getAppenderName())
                    .withLayout(layout)
                    .build();
            return rocketmqExtendToolAppender;
        }

    }

    private static Log createLogger(final String loggerName) {
        Log logger = LogFactory.getLogger(loggerName);
        InnerLogFactory.InnerLog innerLog = (InnerLogFactory.InnerLog) logger;
        Logger realLogger = innerLog.getLogger();
        realLogger.addAppender(appenderProxy);
        realLogger.setLevel(Level.toLevel(ClientLoggerConfig.getLogLevel()));
        realLogger.setAdditive(ClientLoggerConfig.isLogAdditive());
        return logger;
    }

    public static Log getLog() {
        return LOGGER;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/14 16:05
     * @description: Appender的代理类，通过该代理类来调用appender的append和close方法
     */
    private static class AppenderProxy extends Appender {
        private Appender appenderProxy;


        @Override
        public void append(LoggingEvent event) {
            if (null == appenderProxy) {
                appenderProxy = ClientLogger.createClientAppender();
            }
            appenderProxy.doAppend(event);
        }

        @Override
        public void close() {
            if (null != appenderProxy) {
                appenderProxy.close();
            }
        }
    }


}
