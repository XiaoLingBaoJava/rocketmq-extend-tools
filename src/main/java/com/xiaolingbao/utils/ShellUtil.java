package com.xiaolingbao.utils;

import com.alibaba.fastjson.JSONObject;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.*;

/**
 * @author: xiaolingbao
 * @date: 2022/5/29 21:42
 * @description: 
 */
public class ShellUtil {

    private static final Log log = ClientLogger.getLog();

    private static volatile ExecutorService executor;

    /**
     * 执行shell命令并获取输出
     */
    public static String execute(String command) {
        return executeAndGetExitStatus(command).getString("out");
    }

    /**
     * 执行shell命令并获得输出及退出码，失败重试 共尝试retryTimes次
     */
    public static JSONObject executeAndGetExitStatus(String command, int retryTimes) {
        JSONObject result = new JSONObject();

        for (int i = 0; i < retryTimes; i++) {
            result = executeAndGetExitStatus(command);
            if (result.getInteger("exitStatus") != 0) {
                log.info("执行shell错误,再次执行 command: {}, times: {}", command, i);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * 执行命令并获得输出以及退出码
     */
    public static JSONObject executeAndGetExitStatus(String command) {
        JSONObject result = new JSONObject();

        StringBuilder out = new StringBuilder();
        Integer exitStatus = -1;

        ProcessBuilder pb = new ProcessBuilder(new String[] { "/bin/sh", "-c", command });
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append(System.getProperty("line.separator"));
            }
            exitStatus = process.waitFor();

        } catch (Exception e) {
            log.error("执行shell出错, command:{}", command, e);
        }

        result.put("out", out.toString().trim());
        result.put("exitStatus", exitStatus);

        return result;
    }

    public static JSONObject executeAndGetExitStatusInWindows(String command) {
        JSONObject result = new JSONObject();

        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec("cmd /c " + command);
        } catch (IOException e) {
            log.error("执行shell出错, command:{}", command, e);
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error("执行shell出错, command:{}", command, e);
        }
        String line = null;
        StringBuilder build = new StringBuilder();
        while (true) {
            try {
                if (!((line = br.readLine()) != null)) break;
            } catch (IOException e) {
                log.error("执行shell出错, command:{}", command, e);
            }
            build.append(line);
        }
        result.put("out", build.toString().trim());

        return result;
    }

    /**
     * 执行shell命令，设定时间限制为${timeLimit} s
     */
    public static JSONObject executeInLimitTime(String command, long timeLimit) throws TimeoutException {
        JSONObject result = new JSONObject();

        if (executor == null) {
            synchronized (ShellUtil.class) {
                if (executor == null) {
                    executor = Executors.newCachedThreadPool();
                }
            }
        }

        try {
            Future<JSONObject> future = executor.submit(() -> executeAndGetExitStatus(command));
            result = future.get(timeLimit, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            log.debug("执行shell命令超时：command: {}", command);

            // 删除任务
            killProcess(command);

            throw new TimeoutException("执行命令超时");
        } catch (Exception e) {
            log.error("执行shell出错, command:{}", command, e);
        }

        return result;
    }

    /**
     * 杀掉一个shell进程
     *
     * @param commandToKill
     *            源命令
     */
    public static void killProcess(String commandToKill) {
        // 原型：kill -9 `ps -ef | grep "xxx.sh" |grep -v grep | awk -F' ' '{print $2}'`
        log.debug("ExecuteShellCommand: killProcess: " + commandToKill);

        String command = "kill -9 `ps -ef | grep \"" + commandToKill + "\" | grep -v grep | awk -F' ' '{print $2}'`";

        try {
            Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
        } catch (IOException e) {
            log.error("kill 进程出错, command:{}", command, e);
        }
    }
}
