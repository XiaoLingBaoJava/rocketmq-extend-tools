package com.xiaolingbao.scaling;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/28 16:42
 * @description: 
 */
public abstract class ScalingGroup<T> {
    protected String name;

    protected int minInstanceNum;

    protected int maxInstanceNum;

    protected LocalDateTime lastExecuteTime = LocalDateTime.of(1999, Month.JANUARY, 1, 0, 0);

    protected long frozenSeconds;

    protected StartingTemplate<T> startingTemplate;

    protected Function<List<T>, T> removeStrategy;

    public abstract boolean start();

    public abstract T remove();
}
