package com.xiaolingbao.logging.inner;

import java.io.Serializable;

/**
 * @author: xiaolingbao
 * @date: 2022/5/11 9:39
 * @description: 自定义的level类，表示日志等级
 */
public class Level implements Serializable {

    transient int level;
    transient String levelStr;
    transient int syslogEquivalent;

    // OFF表示关闭
    public final static int OFF_INT = Integer.MAX_VALUE;
    public final static int ERROR_INT = 40000;
    public final static int WARN_INT = 30000;
    public final static int INFO_INT = 20000;
    public final static int DEBUG_INT = 10000;
    // ALL表示全部
    public final static int ALL_INT = Integer.MIN_VALUE;


    private static final String ALL_NAME = "ALL";

    private static final String DEBUG_NAME = "DEBUG";

    private static final String INFO_NAME = "INFO";

    private static final String WARN_NAME = "WARN";

    private static final String ERROR_NAME = "ERROR";

    private static final String OFF_NAME = "OFF";

    final static public Level OFF = new Level(OFF_INT, OFF_NAME, 0);

    final static public Level ERROR = new Level(ERROR_INT, ERROR_NAME, 3);

    final static public Level WARN = new Level(WARN_INT, WARN_NAME, 4);

    final static public Level INFO = new Level(INFO_INT, INFO_NAME, 6);

    final static public Level DEBUG = new Level(DEBUG_INT, DEBUG_NAME, 7);

    final static public Level ALL = new Level(ALL_INT, ALL_NAME, 7);

    static final long serialVersionUID = 3491141966387927654L;



    public Level(int level, String levelStr, int syslogEquivalent) {
        this.level = level;
        this.levelStr = levelStr;
        this.syslogEquivalent = syslogEquivalent;
    }

    public static Level toLevel(String levelStr) {
        return toLevel(levelStr, Level.DEBUG);
    }

    public static Level toLevel(int val) {
        return toLevel(val, Level.DEBUG);
    }



    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 9:49
     * @param levelIntValue level对应的int值
     * @param defaultLevel
     * @return com.xiaolingbao.logging.inner.Level Level对象
     * @description: 通过level对应的int值返回其对应的level对象
     */
    public static Level toLevel(int levelIntValue, Level defaultLevel) {
        switch (levelIntValue) {
            case ALL_INT:
                return Level.ALL;
            case DEBUG_INT:
                return Level.DEBUG;
            case INFO_INT:
                return Level.INFO;
            case WARN_INT:
                return Level.WARN;
            case ERROR_INT:
                return Level.ERROR;
            case OFF_INT:
                return Level.OFF;
            default:
                return defaultLevel;
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 9:54
     * @param LevelName level对应的String
     * @param defaultLevel
     * @return com.xiaolingbao.logging.inner.Level Level对象
     * @description: 通过Level的String值返回Level对象
     */
    public static Level toLevel(String LevelName, Level defaultLevel) {
        if (LevelName == null) {
            return defaultLevel;
        }
        String s = LevelName.toUpperCase();
        if (s.equals(ALL_NAME)) {
            return Level.ALL;
        }
        if (s.equals(DEBUG_NAME)) {
            return Level.DEBUG;
        }
        if (s.equals(INFO_NAME)) {
            return Level.INFO;
        }
        if (s.equals(WARN_NAME)) {
            return Level.WARN;
        }
        if (s.equals(ERROR_NAME)) {
            return Level.ERROR;
        }
        if (s.equals(OFF_NAME)) {
            return Level.OFF;
        }
        if (s.equals(INFO_NAME)) {
            return Level.INFO;
        }
        return defaultLevel;


    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 10:00
     * @param o
     * @return boolean
     * @description: 只要两个Level对象的level int值相等，则equal
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Level) {
            Level l = (Level) o;
            return this.level == l.level;
        }
        return false;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 10:03
     * @return int
     * @description: 只要level int和levelStr相同，两个对象的hashCode就相同
     */
    @Override
    public int hashCode() {
        int hashCode = level;
        hashCode = 31 * level + (levelStr != null ? levelStr.hashCode() : 0);
        hashCode = 31 * hashCode + syslogEquivalent;
        return hashCode;
    }

    public boolean isGreaterOrEqual(Level r) {
        return level >= r.level;
    }

    @Override
    final public String toString() {
        return levelStr;
    }

    public final int toInt() {
        return level;
    }

}
