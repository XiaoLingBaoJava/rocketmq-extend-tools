package com.xiaolingbao.scaling.strategy;

import java.util.List;
import java.util.function.Function;

/**
 * @author: xiaolingbao
 * @date: 2022/5/28 20:11
 * @description: 
 */
public interface RemoveStrategy<T> {
    Function<List<T>, T> getNewestRemoveStrategy();

    Function<List<T>, T> getOldestRemoveStrategy();
}
