package com.xiaolingbao.logging.inner;

import java.io.*;
import java.util.ArrayList;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:29
 * @description: 表示日志事件，appender会处理日志事件
 */
public class LoggingEvent implements java.io.Serializable {

    public final transient String fqcnOfCategoryClass;

    private transient Object message;

    private transient Level level;

    private transient Logger logger;

    // renderedMessage就是把message的回车和换行符去掉
    private String renderedMessage;

    private String threadName;

    // 创建LoggingEvent时的时间戳
    public final long timeStamp;

    private Throwable throwable;

    public LoggingEvent(String fqcnOfCategoryClass, Logger logger,
                        Level level, Object message, Throwable throwable) {
        this.fqcnOfCategoryClass = fqcnOfCategoryClass;
        this.message = message;
        this.logger = logger;
        this.throwable = throwable;
        this.level = level;
        timeStamp = System.currentTimeMillis();
    }

    public Object getMessage() {
        if (message != null) {
            return message;
        } else {
            return getRenderedMessage();
        }
    }

    public String getRenderedMessage() {
        if (renderedMessage == null && message != null) {
            if (message instanceof String) {
                renderedMessage = (String) message;
            } else {
                renderedMessage = message.toString();
            }
            if (renderedMessage != null) {
                renderedMessage = renderedMessage.replace('\r', ' ').replace('\n', ' ');
            }
        }
        return renderedMessage;
    }

    public String getThreadName() {
        if (threadName == null) {
            threadName = (Thread.currentThread()).getName();
        }
        return threadName;
    }

    public Level getLevel() {
        return level;
    }

    public String getLoggerName() {
        return logger.getName();
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 16:21
     * @return java.lang.String[]
     * @description: 读取throwable的stackTrace，并返回
     */
    public String[] getThrowableStr() {
        if (throwable == null) {
            return null;
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        try {
            throwable.printStackTrace(printWriter);
        } catch (RuntimeException e) {
            SysLogger.warn("日志系统打印报错stack trace出错", e);
        }
        printWriter.flush();

        LineNumberReader reader = new LineNumberReader(new StringReader(stringWriter.toString()));
        ArrayList<String> lines = new ArrayList<>();
        try {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            if (e instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(e.toString());
        }
        String[] throwableStr = new String[lines.size()];
        lines.toArray(throwableStr);
        return throwableStr;
    }


}
