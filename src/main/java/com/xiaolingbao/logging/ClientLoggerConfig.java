package com.xiaolingbao.logging;
/**
 * @author: xiaolingbao
 * @date: 2022/5/15 11:08
 * @description: 
 */
public final class ClientLoggerConfig {
    private static String logRoot = System.getProperty("user.home") + "/rocketmq_extend_tools/logs";
    // 日志文件大小为10M
    private static String logFileSize = "1073741824";
    private static String logLevel = "ALL";
    private static boolean logAdditive = true;
    private static String clientLogFileName = "rocketmq_extend_tool.log";
    private static int logAsyncQueueSize = 1024;
    private static String appenderName = "rocketmqExtendToolAppender";
    private static String loggerName = "RocketmqExtendTool";
    private static boolean useFileLog = false;
    private static int logMaxFileIndex = 10;

    public static String getLogRoot() {
        return logRoot;
    }

    public static void setLogRoot(String logRoot) {
        ClientLoggerConfig.logRoot = logRoot;
    }

    public static String getLogFileSize() {
        return logFileSize;
    }

    public static void setLogFileSize(String logFileSize) {
        ClientLoggerConfig.logFileSize = logFileSize;
    }

    public static String getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(String logLevel) {
        ClientLoggerConfig.logLevel = logLevel;
    }

    public static boolean isLogAdditive() {
        return logAdditive;
    }

    public static void setLogAdditive(boolean logAdditive) {
        ClientLoggerConfig.logAdditive = logAdditive;
    }

    public static String getClientLogFileName() {
        return clientLogFileName;
    }

    public static void setClientLogFileName(String clientLogFileName) {
        ClientLoggerConfig.clientLogFileName = clientLogFileName;
    }

    public static int getLogAsyncQueueSize() {
        return logAsyncQueueSize;
    }

    public static void setLogAsyncQueueSize(int logAsyncQueueSize) {
        ClientLoggerConfig.logAsyncQueueSize = logAsyncQueueSize;
    }

    public static String getAppenderName() {
        return appenderName;
    }

    public static void setAppenderName(String appenderName) {
        ClientLoggerConfig.appenderName = appenderName;
    }

    public static String getLoggerName() {
        return loggerName;
    }

    public static void setLoggerName(String loggerName) {
        ClientLoggerConfig.loggerName = loggerName;
    }

    public static boolean isUseFileLog() {
        return useFileLog;
    }

    public static void setUseFileLog(boolean useFileLog) {
        ClientLoggerConfig.useFileLog = useFileLog;
    }

    public static int getLogMaxFileIndex() {
        return logMaxFileIndex;
    }

    public static void setLogMaxFileIndex(int logMaxFileIndex) {
        ClientLoggerConfig.logMaxFileIndex = logMaxFileIndex;
    }
}
