package com.xiaolingbao.logging;


import org.junit.Test;

public class ClientLoggerTest {
    private final Log log = ClientLogger.getLog();

    private String testMsg = "test";
    private String testMsg2 = "test2";

    @Test
    public void logTest() throws InterruptedException {
        log.error("error log test");
        log.error("错误日志测试");
        log.error("with Exception log test", new RuntimeException());
        log.error("with Exception log test2: {}", testMsg, new RuntimeException());
        log.error("error messgae: {}", testMsg);

        log.info("test message: {}", testMsg);
        log.info("info messgae1: {} message2: {}", testMsg, testMsg2);

        log.warn("warn log test");
        log.warn("warn log test with exception", new IllegalStateException());
        log.warn("warn message1: {}, message2: {}", testMsg, testMsg2);

        log.debug("debug log test");
        log.debug("debug message: {}", testMsg);

        Thread.sleep(5000);
    }

    @Test
    public void fileLogTest() throws InterruptedException {
        ClientLoggerConfig.setUseFileLog(true);
        Log fileLog = ClientLogger.getLog();

        log.error("error log test");
        log.error("错误日志测试");
        log.error("with Exception log test", new RuntimeException());
        log.error("error messgae: {}", testMsg);

        log.info("test message: {}", testMsg);
        log.info("info messgae1: {} message2: {}", testMsg, testMsg2);

        log.warn("warn log test");
        log.warn("warn log test with exception", new IllegalStateException());
        log.warn("warn message1: {}, message2: {}", testMsg, testMsg2);

        log.debug("debug log test");
        log.debug("debug message: {}", testMsg);

        Thread.sleep(5000);
    }

}