package com.xiaolingbao.scaling.monitor;
/**
 * @author: xiaolingbao
 * @date: 2022/5/29 8:42
 * @description: 
 */
public enum MonitorMethod {
    AVG(1),
    MAX(2),
    MIN(3),
    CURRENT(4)
    ;

    private int method;

    MonitorMethod(int method) {
        this.method = method;
    }

    public int getMethod() {
        return method;
    }
}
