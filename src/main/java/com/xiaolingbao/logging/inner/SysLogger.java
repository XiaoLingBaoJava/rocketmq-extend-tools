package com.xiaolingbao.logging.inner;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:32
 * @description: 在显示台显示日志信息的Logger
 */
public class SysLogger {

    private static boolean debugEnabled = false;

    private static boolean quietMode = false;

    private static final String INFO_PREFIX = "RocketmqExtendToolsLog:INFO ";
    private static final String DEBUG_PREFIX = "RocketmqExtendToolsLog:DEBUG ";
    private static final String ERR_PREFIX = "RocketmqExtendToolsLog:ERROR ";
    private static final String WARN_PREFIX = "RocketmqExtendToolsLog:WARN ";

    public static void setDebugEnabled(boolean enable) {
        debugEnabled = enable;
    }

    public static void setQuietMode(boolean quietMode) {
        SysLogger.quietMode = quietMode;
    }

    public static void info(String msg) {
        if (quietMode) {
            return;
        }
        System.out.println(INFO_PREFIX + msg);
    }

    public static void info(String msg, Throwable t) {
        if (quietMode) {
            return;
        }
        System.out.println(INFO_PREFIX + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    public static void debug(String msg) {
        if (quietMode || !debugEnabled) {
            return;
        }
        System.out.println(DEBUG_PREFIX + msg);
    }

    public static void debug(String msg, Throwable t) {
        if (quietMode || !debugEnabled) {
            return;
        }
        System.out.println(DEBUG_PREFIX + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    public static void warn(String msg) {
        if (quietMode) {
            return;
        }
        System.out.println(WARN_PREFIX + msg);
    }

    public static void warn(String msg, Throwable t) {
        if (quietMode) {
            return;
        }
        System.out.println(WARN_PREFIX + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    public static void error(String msg) {
        if (quietMode) {
            return;
        }
        System.out.println(ERR_PREFIX + msg);
    }

    public static void error(String msg, Throwable t) {
        if (quietMode) {
            return;
        }
        System.out.println(ERR_PREFIX + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }





}
