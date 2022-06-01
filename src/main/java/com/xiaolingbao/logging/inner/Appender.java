package com.xiaolingbao.logging.inner;

import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:17
 * @description: 与将日志写入文件底层操作有关
 */

public abstract class Appender {

    public static final int CODE_WRITE_FAILURE = 1;
    public static final int CODE_FLUSH_FAILURE = 2;
    public static final int CODE_CLOSE_FAILURE = 3;
    public static final int CODE_FILE_OPEN_FAILURE = 4;

    public final static String LINE_SEP = System.getProperty("line.separator");

    protected String name;

    protected Layout layout;

    private boolean firstTime = true;

    protected boolean closed = false;

    public void activateOptions() {

    }

    public abstract void append(LoggingEvent event);

    public abstract void close();

    /**
     * @author: xiaolingbao
     * @date: 2022/5/10 16:31
     * @description: 主动调用GC
     */
    @Override
    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            SysLogger.error("GC回收名为" + name + "的Appender失败", throwable);
        }
        if (this.closed) {
            return;
        }

        SysLogger.debug("GC回收名为" + name + "的Appender成功");
        close();
    }

    public synchronized void doAppend(LoggingEvent event) {
        if (closed) {
            SysLogger.error("尝试用一个已经关闭的appender去append,appender名: [" + name + "]");
            return;
        }
        this.append(event);
    }


    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public final String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void handleError (String message, Exception e, int errorCode) {
        if (e instanceof InterruptedIOException || e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        if (firstTime) {
            SysLogger.error(message + " errorCode:" + errorCode, e);
            firstTime = false;
        }
    }

    public void handleError(String message) {
        if (firstTime) {
            SysLogger.error(message);
            firstTime = false;
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/10 21:23
     * @description: 内部接口,包含对Appender集合的一些操作
     */
    public interface AppenderPipeline {
        void addAppender(Appender newAppender);

        Iterator getAllAppenders();

        Appender getAppender(String name);

        boolean isAttached(Appender appender);

        void removeAllAppenders();

        void removeAppender(Appender appender);

        void removeAppender(String name);

    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/10 21:29
     * @description: 内部封装了Appender的list,实现AppenderPipeline接口，能对集合CRUD
     */
    public static class AppenderPipelineImpl implements AppenderPipeline {
        private Vector<Appender> appenderList;

        @Override
        public void addAppender(Appender newAppender) {
            if (newAppender == null) {
                return;
            }
            if (appenderList == null) {
                appenderList = new Vector<>(1);
            }
            if (!appenderList.contains(newAppender)) {
                appenderList.addElement(newAppender);
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/10 21:36
         * @param event 日志事件
         * @return int appendList中appender的数量
         * @description: 遍历集合，集合中每个appender都调用append方法
         */
        public int appendLoopOnAppenders(LoggingEvent event) {
            int size = 0;

            if (appenderList != null) {
                size = appenderList.size();
                appenderList.forEach(ap -> {
                    ap.doAppend(event);
                });
            }

            return size;
        }

        @Override
        public Iterator getAllAppenders() {
            if (appenderList == null) {
                return null;
            }
            return appenderList.iterator();
        }

        @Override
        public Appender getAppender(String name) {
            if (appenderList == null || name == null) {
                return null;
            }
            for (Appender ap : appenderList) {
                if (name.equals(ap.getName())) {
                    return ap;
                }
            }
            return null;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/10 21:43
         * @param appender
         * @return 参数appender是否在list中，在返回true，不在返回false
         * @description: 判断appender是否在list中
         */
        @Override
        public boolean isAttached(Appender appender) {
            if (appenderList == null || appender == null) {
                return false;
            }
            for (Appender ap : appenderList) {
                if (ap == appender) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/10 21:46
         * @description: 把appenderList中的所有appender全部关闭，然后删除所有appender，最后把appenderList置null
         */
        @Override
        public void removeAllAppenders() {
            if (appenderList == null) {
                return;
            }
            appenderList.forEach(ap -> {
                ap.close();
            });
            appenderList.removeAllElements();
            appenderList = null;
        }

        @Override
        public void removeAppender(Appender appender) {
            if (appender == null || appenderList == null) {
                return;
            }
            appenderList.removeElement(appender);
        }

        @Override
        public void removeAppender(String name) {
            if (name == null || appenderList == null) {
                return;
            }
            for (Appender appender : appenderList) {
                if (name.equals(appender.getName())) {
                    appenderList.removeElement(appender);
                    return;
                }
            }
        }
    }

}
