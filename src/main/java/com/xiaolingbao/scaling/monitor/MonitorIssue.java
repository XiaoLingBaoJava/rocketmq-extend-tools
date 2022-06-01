package com.xiaolingbao.scaling.monitor;
/**
 * @author: xiaolingbao
 * @date: 2022/5/29 8:26
 * @description: 
 */
public enum MonitorIssue {
    CPU(1),
    MEMORY(2),
    ProduceTPS(3),
    ConsumeTPS(4)
    ;

    private int issue;

    MonitorIssue(int issue) {
        this.issue = issue;
    }

    public int getIssue() {
        return issue;
    }
}
