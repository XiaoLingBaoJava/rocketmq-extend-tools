package com.xiaolingbao.scaling.monitor;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author: xiaolingbao
 * @date: 2022/5/30 15:01
 * @description: 
 */
public class MonitorManager {

    private Log log = ClientLogger.getLog();

    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    static {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
    }

    // key: monitorName
    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    public boolean startMonitor(AbstractMonitor monitor) {
        if (futureMap.containsKey(monitor.monitorName)) {
            log.warn("与monitorName相同的Monitor已经存在, monitorName: {}", monitor.monitorName);
            return false;
        }
        ScheduledFuture<?> future = threadPoolTaskScheduler.schedule(monitor, new CronTrigger(monitor.cron));
        futureMap.put(monitor.monitorName, future);
        log.info("启动monitor成功, monitorName: {}, scalingGroup: {}", monitor.monitorName, monitor.scalingGroup);
        return true;
    }

    public boolean stopMonitor(String monitorName) {
        ScheduledFuture<?> future = futureMap.get(monitorName);
        if (future != null) {
            future.cancel(true);
            futureMap.remove(monitorName);
            log.info("关闭monitor成功, monitorName: {}", monitorName);
            return true;
        } else {
            log.warn("关闭monitor失败, 不存在名为: {}的monitor", monitorName);
            return false;
        }
    }

}
