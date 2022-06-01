package com.xiaolingbao.logging.inner;
/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:25
 * @description: 日志输出结果样式相关类
 */
public abstract class Layout {
    public abstract String format(LoggingEvent event);

    public String getContentType() {
        return "text/plain";
    }

    public String getHeader() {
        return null;
    }

    public String getFooter() {
        return null;
    }

    public abstract boolean ignoreThrowable();
}