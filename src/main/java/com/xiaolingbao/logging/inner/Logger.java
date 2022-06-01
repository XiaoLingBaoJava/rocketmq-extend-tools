package com.xiaolingbao.logging.inner;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;


/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:16
 * @description: 真正进行日志操作的类，实现了Appender的AppenderPipeline接口
 */

public class Logger implements Appender.AppenderPipeline {

    // FQCN表示Full Qualified Class Name,即全类名
    private static final String FQCN = Logger.class.getName();

    private static final DefaultLoggerRepository REPOSITORY = new DefaultLoggerRepository(new RootLogger(Level.DEBUG));

    public static LoggerRepository getRepository() {
        return REPOSITORY;
    }

    private String name;

    private volatile Level level;

    private volatile Logger parent;

    Appender.AppenderPipelineImpl appenderPipeline;

    private boolean additive = true;

    private Logger(String name) {
        this.name = name;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 15:11
     * @param name 类名
     * @return com.xiaolingbao.logging.inner.Logger
     * @description: 外部获取logger的接口，实际上是从LoggerRepository中的map中获取
     */
    public static Logger getLogger(String name) {
        return getRepository().getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return getRepository().getLogger(clazz.getName());
    }

    public static Logger getRootLogger() {
        return getRepository().getRootLogger();
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    public final String getName() {
        return name;
    }

    public final Level getLevel() {
        return this.level;
    }

    public Level getEffectiveLevel() {
        for (Logger logger = this; logger != null; logger = logger.parent) {
            if (logger.level != null) {
                return logger.level;
            }
        }
        return null;
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 15:02
     * @description: 关闭appenderPipeline中的所有appender
     */
    private synchronized void closeNestedAppenders() {
        Iterator iterator = this.getAllAppenders();
        if (iterator != null) {
            while (iterator.hasNext()) {
                Appender appender = (Appender) iterator.next();
                if (appender instanceof Appender.AppenderPipeline) {
                    appender.close();
                }
            }
        }
    }


    @Override
    public void addAppender(Appender newAppender) {
        if (appenderPipeline == null) {
            appenderPipeline = new Appender.AppenderPipelineImpl();
        }
        appenderPipeline.addAppender(newAppender);
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 15:20
     * @param event
     * @description: logger的appenderPipeline中的所有appender执行append操作
     *               若additive为true,则logger的parent也要做同样处理。
     *               若additive为false，则logger的parent无需做处理
     *               additive默认为true.
     *               若没有发现任何appender，则打印warning
     */
    public void callAppenders(LoggingEvent event) {
        int writes = 0;
        synchronized (this) {
            if (this.appenderPipeline != null) {
                writes += this.appenderPipeline.appendLoopOnAppenders(event);
            }
        }

        if (writes == 0) {
            getRepository().emitNoAppenderWarning(this);
        }
    }

    @Override
    public Iterator getAllAppenders() {
        if (appenderPipeline == null) {
            return null;
        } else {
            return appenderPipeline.getAllAppenders();
        }
    }

    @Override
    public Appender getAppender(String name) {
        if (appenderPipeline == null || name == null) {
            return null;
        }
        return appenderPipeline.getAppender(name);
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 15:34
     * @param appender
     * @return boolean
     * @description: 判断参数appender是否在appenderPipeline封装的list中
     */
    @Override
    public boolean isAttached(Appender appender) {
        return appender != null && appenderPipeline != null && appenderPipeline.isAttached(appender);
    }

    @Override
    public void removeAllAppenders() {
        if (appenderPipeline != null) {
            appenderPipeline.removeAllAppenders();
            appenderPipeline = null;
        }
    }

    @Override
    public void removeAppender(Appender appender) {
        if (appender == null || appenderPipeline == null) {
            return;
        }
        appenderPipeline.removeAppender(appender);
    }

    @Override
    public void removeAppender(String name) {
        if (name == null || appenderPipeline == null) {
            return;
        }
        appenderPipeline.removeAppender(name);
    }

    public void debug(Object message) {
        if (getRepository().isDisabled(Level.DEBUG_INT)) {
            return;
        }
        if (Level.DEBUG.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.DEBUG, message, null);
        }
    }

    public void debug(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.DEBUG_INT)) {
            return;
        }
        if (Level.DEBUG.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.DEBUG, message, t);
        }
    }

    public void error(Object message) {
        if (getRepository().isDisabled(Level.ERROR_INT)) {
            return;
        }
        if (Level.ERROR.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.ERROR, message, null);
        }
    }

    public void error(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.ERROR_INT)) {
            return;
        }
        if (Level.ERROR.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.ERROR, message, t);
        }
    }

    public void warn(Object message) {
        if (getRepository().isDisabled(Level.WARN_INT)) {
            return;
        }
        if (Level.WARN.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.WARN, message, null);
        }
    }

    public void warn(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.WARN_INT)) {
            return;
        }
        if (Level.WARN.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.WARN, message, t);
        }
    }

    public void info(Object message) {
        if (getRepository().isDisabled(Level.INFO_INT)) {
            return;
        }
        if (Level.INFO.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.INFO, message, null);
        }
    }

    public void info(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.INFO_INT)) {
            return;
        }
        if (Level.INFO.isGreaterOrEqual(this.getEffectiveLevel())) {
            doLog(FQCN, Level.INFO, message, t);
        }
    }



    private void doLog(String fqcn, Level level, Object message, Throwable t) {
        callAppenders(new LoggingEvent(fqcn, this, level, message, t));
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/10 22:02
     * @description: 内部接口,保存logger的map
     */
    public interface LoggerRepository {
        boolean isDisabled(int level);

        void setLogLevel(Level level);

        void emitNoAppenderWarning(Logger logger);

        Level getLogLevel();

        Logger getLogger(String name);

        Logger getRootLogger();

        Logger exists(String name);

        void shutdown();

        Iterator getCurrentLoggers();

    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 10:33
     * @description: 内部封装了一个logger map,key为类名，value为对应的logger.该类的方法主要是该map的维护和CRUD
     */
    public static class DefaultLoggerRepository implements LoggerRepository {

        // 保存类名到logger的映射
        private final Hashtable<CategoryKey, Object> loggerTable = new Hashtable<>();

        private Logger root;

        private int logLevelInt;

        private Level logLevel;

        boolean emittedNoAppenderWarning = false;

        public DefaultLoggerRepository(Logger root) {
            this.root = root;
            setLogLevel(Level.ALL);
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 14:55
         * @param level
         * @return boolean
         * @description: 通过不同日志level对应的int值来进行比较，越严重的级别
         *               int值越大，例如error级别就比warn级别大
         */
        @Override
        public boolean isDisabled(int level) {
            return logLevelInt > level;
        }

        @Override
        public void setLogLevel(Level level) {
            if (level != null) {
                logLevelInt = level.level;
                logLevel = level;
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 10:52
         * @param logger
         * @description: 发出没有appender的warning，只会执行一次
         */
        @Override
        public void emitNoAppenderWarning(Logger logger) {
            if (!this.emittedNoAppenderWarning) {
                SysLogger.warn("名为" + logger.getName() + "的logger没有对应的appender,请检查是否正确初始化了日志系统");
                this.emittedNoAppenderWarning = true;
            }
        }

        @Override
        public Level getLogLevel() {
            return logLevel;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 14:46
         * @param name
         * @return com.xiaolingbao.logging.inner.Logger
         * @description: 通过类名来获取logger,若loggerTable中没有该logger，
         *               则创建logger并放入table中，同时更新parent关系.
         *               若table中已有该logger，则直接返回。若table中已有
         *               该类的下级类名（例如内部类）的logger，则创建该类对应
         *               的logger并放入table中，同时更新parent和children关系
         */
        @Override
        public Logger getLogger(String name) {
            CategoryKey key = new CategoryKey(name);
            Logger logger;

            synchronized (loggerTable) {
                Object o = loggerTable.get(key);
                if (o == null) {
                    logger = makeNewLoggerInstance(name);
                    loggerTable.put(key, logger);
                    updateParents(logger);
                    return logger;
                } else if (o instanceof Logger) {
                    return (Logger) o;
                } else if (o instanceof ProvisionNode) {
                    logger = makeNewLoggerInstance(name);
                    loggerTable.put(key, logger);
                    updateChildren((ProvisionNode) o, logger);
                    updateParents(logger);
                    return logger;
                } else {
                    return null;
                }
            }
        }

        public Logger makeNewLoggerInstance(String name) {
            return new Logger(name);
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 11:23
         * @param logger
         * @description: 根据logger的全类名，在loggerTable中去寻找它的上一级logger，也就是它的parent,
         *               若logger的上一级类名对应的logger为null，则将该logger添加到vector中, 并将vector 作为进上一级类名对应的value,
         *               直到找到了存在对应logger的上级类名，此时将logger的parent设为它，并退出该方法.
         *               若发现上级类名对应的value为vector，则将该logger放入vector中.
         *               若直到最后也没找到存在对应logger的上级类，则将logger的parent设为root.
         *
         *               换句话说，这个方法会将没有创建logger的类，把它对应的logger设置为其内部类的vector
         */
        private void updateParents(Logger logger) {
            String name = logger.name;
            int length = name.length();
            boolean parentFound = false;

            for (int i = name.lastIndexOf('.', length - 1); i >= 0;
                 i = name.lastIndexOf('.', i - 1)) {
                String substr = name.substring(0, i);
                CategoryKey key = new CategoryKey(substr);
                Object o = loggerTable.get(key);
                if (o == null) {
                    loggerTable.put(key, new ProvisionNode(logger));
                } else if (o instanceof Logger) {
                    parentFound = true;
                    logger.parent = (Logger) o;
                    break;
                } else if (o instanceof ProvisionNode) {
                    ((ProvisionNode) o).addElement(logger);
                } else {
                    Exception e = new IllegalStateException("在loggerTable中出现了异常的对象类型,该对象的类名为:" + o.getClass());
                    e.printStackTrace();
                }
            }
            if (!parentFound) {
                logger.parent = root;
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 14:41
         * @param provisionNode
         * @param logger
         * @description: 遍历provisionNode,若其中元素的parent name不以logger name起始，
         *               则需更新它们的parent为logger,并将其中元素的parent赋给logger.parent
         */
        private void updateChildren(ProvisionNode provisionNode, Logger logger) {
            for (Logger l : provisionNode) {
                if (!l.parent.name.startsWith(logger.name)) {
                    logger.parent = l.parent;
                    l.parent = logger;
                }
            }
        }

        @Override
        public Logger getRootLogger() {
            return root;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 10:57
         * @param name
         * @return com.xiaolingbao.logging.inner.Logger
         * @description: 查找HashTable中是否存在指定name的Logger,并将其返回,不存在则返回null
         */
        @Override
        public Logger exists(String name) {
            Object o = loggerTable.get(new CategoryKey(name));
            if (o instanceof Logger) {
                return (Logger) o;
            }
            return null;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 15:04
         * @description: 将root logger以及loggerTable中的所有logger的appender全部关闭
         */
        @Override
        public void shutdown() {
            Logger root = getRootLogger();
            root.closeNestedAppenders();

            synchronized (loggerTable) {
                Iterator iterator = this.getCurrentLoggers();
                while (iterator.hasNext()) {
                    Logger logger = (Logger) iterator.next();
                    logger.closeNestedAppenders();
                }
                root.removeAllAppenders();
            }
        }

        @Override
        public Iterator getCurrentLoggers() {
            Vector<Logger> loggers = new Vector<>(loggerTable.size());

            Iterator iterator = loggers.iterator();
            while (iterator.hasNext()) {
                Object o = iterator.next();
                if (o instanceof Logger) {
                    Logger logger = (Logger) o;
                    loggers.addElement(logger);
                }
            }
            return loggers.iterator();
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 10:34
         * @description: 封装了Logger的name和hashCode，作为hashTabel的key
         */
        private class CategoryKey {
            private String name;
            private int nameHashCode;

            public CategoryKey(String name) {
                this.name = name;
                this.nameHashCode = name.hashCode();
            }

            @Override
            public final int hashCode() {
                return nameHashCode;
            }

            @Override
            public final boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o != null && o instanceof CategoryKey) {
                    CategoryKey categoryKey = (CategoryKey) o;
                    return name.equals(categoryKey.name);
                }
                return false;
            }
        }
    }

    public static class RootLogger extends Logger {
        public RootLogger(Level level) {
            super("root");
            setLevel(level);
        }
    }

    public static class ProvisionNode extends Vector<Logger> {
        public ProvisionNode(Logger logger) {
            super();
            addElement(logger);
        }
    }

}
