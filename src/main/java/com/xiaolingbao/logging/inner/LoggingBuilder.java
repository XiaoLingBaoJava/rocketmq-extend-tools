package com.xiaolingbao.logging.inner;

import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 16:35
 * @description: 
 */
public class LoggingBuilder {

    public static final String SYSTEM_OUT = "System.out";
    public static final String SYSTEM_ERR = "System.err";

    public static String ENCODING =  "UTF-8";

    public static String getEncoding() {
        return ENCODING;
    }

    public static void setEncoding(String ENCODING) {
        LoggingBuilder.ENCODING = ENCODING;
    }

    public static AppenderBuilder newAppenderBuilder() {
        return new AppenderBuilder();
    }

    public static class AppenderBuilder {
        private AsyncAppender asyncAppender;

        private Appender appender = null;

        private AppenderBuilder() {

        }

        public AppenderBuilder withLayout(Layout layout) {
            appender.setLayout(layout);
            return this;
        }

        public AppenderBuilder withName(String name) {
            appender.setName(name);
            return this;
        }

        public AppenderBuilder withConsoleAppender(String target) {
            ConsoleAppender consoleAppender = new ConsoleAppender();
            consoleAppender.setTarget(target);
            consoleAppender.activateOptions();
            this.appender = consoleAppender;
            return this;
        }

        public AppenderBuilder withFileAppender(String file) {
            FileAppender appender = new FileAppender();
            appender.setFile(file);
            appender.setFileAppend(true);
            appender.setBufferedIO(false);
            appender.setEncoding(ENCODING);
            appender.setImmediateFlush(true);
            appender.activateOptions();
            this.appender = appender;
            return this;
        }

        public AppenderBuilder withRollingFileAppender(String file, String maxFileSize, int maxFileIndex) {
            RollingFileAppender appender = new RollingFileAppender();
            appender.setFile(file);
            appender.setFileAppend(true);
            appender.setBufferedIO(false);
            appender.setEncoding(ENCODING);
            appender.setImmediateFlush(true);
            appender.setMaxFileSize(Integer.parseInt(maxFileSize));
            appender.setMaxBackupIndex(maxFileIndex);
            appender.activateOptions();
            this.appender = appender;
            return this;
        }

        public AppenderBuilder withAsync(boolean blocking, int bufferSize) {
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setBlocking(blocking);
            asyncAppender.setBufferSize(bufferSize);
            this.asyncAppender = asyncAppender;
            return this;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/12 20:31
         * @return com.xiaolingbao.logging.inner.Appender
         * @description: 在build前，必须指定appender，若asyncAppender不为null，
         *               则将appender加入asyncAppender的AppenderPipeline,并
         *               返回asyncAppender。若asyncAppender为null，则返回appender
         */
        public Appender build() {
            if (appender == null) {
                throw new RuntimeException("请先指定appender");
            }
            if (asyncAppender != null) {
                asyncAppender.addAppender(appender);
                return asyncAppender;
            } else {
                return appender;
            }
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/12 20:19
     * @description: AsyncAppender内部封装了appenderPipeline，可以向appenderPipeline中加入各种appender
     */
    public static class AsyncAppender extends Appender implements Appender.AppenderPipeline {

        public static final int DEFAULT_BUFFER_SIZE = 128;

        // 保存LoggingEvent的buffer
        private final List<LoggingEvent> buffer = new ArrayList<>();

        // 保存被丢弃的信息,key为loggerName,值为DiscardSummary
        private final Map<String, DiscardSummary> discardMap = new HashMap<>();

        private int bufferSize = DEFAULT_BUFFER_SIZE;

        private final AppenderPipelineImpl appenderPipeline;

        private final Thread dispatcher;

        private boolean blocking = true;

        /**
         * @author: xiaolingbao
         * @date: 2022/5/12 19:23
         * @description: AsyncAppender的构造函数，会启动一个Dispatcher线程，以后台方式运行
         */
        public AsyncAppender() {
            appenderPipeline = new AppenderPipelineImpl();

            dispatcher = new Thread(new Dispatcher(this, buffer, discardMap, appenderPipeline));
            dispatcher.setDaemon(true);
            dispatcher.setName("AsyncAppender-Dispatcher-" + dispatcher.getName());
            dispatcher.start();
        }




        /**
         * @author: xiaolingbao
         * @date: 2022/5/12 19:44
         * @param event
         * @description: 若dispatcher线程为null或线程未存活，或设置的bufferSize值小于等于0,
         *               则直接调用appenderPipeline的appendLoopOnAppenders处理LoggingEvent.
         *               否则进行下一步判断，判断保存LoggingEvent的buffer的size是否小于设定的最大值，
         *               若小于，则将LoggingEvent加入buffer,若这是第一个loggingEvent，则通知等待的线程
         *               若大于，则已经超过了设定的最大size，若blocking为true，则进行等待，
         *               否则将该LoggingEvent丢弃，封装并保存到discardMap中
         *
         */
        @Override
        public void append(LoggingEvent event) {
            if ((dispatcher == null) || !dispatcher.isAlive() || (bufferSize <= 0)) {
                synchronized (appenderPipeline) {
                    appenderPipeline.appendLoopOnAppenders(event);
                }

                return;
            }

            event.getThreadName();
            event.getRenderedMessage();

            synchronized (buffer) {
                while (true) {
                    int previousSize = buffer.size();
                    if (previousSize < bufferSize) {
                        buffer.add(event);
                        if (previousSize == 0) {
                            buffer.notifyAll();
                        }
                        break;
                    }

                    boolean discard = true;
                    if (blocking && !Thread.interrupted() && Thread.currentThread() != dispatcher) {
                        try {
                            buffer.wait();
                            discard = false;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (discard) {
                        String loggerName = event.getLoggerName();
                        DiscardSummary summary = discardMap.get(loggerName);
                        if (summary == null) {
                            summary = new DiscardSummary(event);
                            discardMap.put(loggerName, summary);
                        } else {
                            summary.add(event);
                        }

                        break;
                    }
                }

            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/12 20:00
         * @description: 关闭AsyncAppender,会将Appender的标志位closed设为true，
         *               将dispatcher线程关闭，将appenderPipeline中的所有appender关闭
         */
        @Override
        public void close() {
            synchronized (buffer) {
                closed = true;
                buffer.notifyAll();
            }

            try {
                dispatcher.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SysLogger.error("在等待dispatcher线程关闭时遇到了InterruptedException", e);
            }

            synchronized (appenderPipeline) {
                Iterator iterator = appenderPipeline.getAllAppenders();
                if (iterator != null) {
                    while (iterator.hasNext()) {
                        Object o = iterator.next();
                        if (o instanceof Appender) {
                            ((Appender) o).close();
                        }
                    }
                }
            }
        }

        @Override
        public void addAppender(final Appender newAppender) {
            synchronized (appenderPipeline) {
                appenderPipeline.addAppender(newAppender);
            }
        }

        @Override
        public Iterator getAllAppenders() {
            synchronized (appenderPipeline) {
                return appenderPipeline.getAllAppenders();
            }
        }

        @Override
        public Appender getAppender(final String name) {
            synchronized (appenderPipeline) {
                return appenderPipeline.getAppender(name);
            }
        }

        @Override
        public boolean isAttached(Appender appender) {
            synchronized (appenderPipeline) {
                return appenderPipeline.isAttached(appender);
            }
        }

        @Override
        public void removeAllAppenders() {
            synchronized (appenderPipeline) {
                appenderPipeline.removeAllAppenders();
            }
        }

        @Override
        public void removeAppender(Appender appender) {
            synchronized (appenderPipeline) {
                appenderPipeline.removeAppender(appender);
            }
        }

        @Override
        public void removeAppender(String name) {
            synchronized (appenderPipeline) {
                appenderPipeline.removeAppender(name);
            }
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(final int size) {
            if (size < 0) {
                throw new NegativeArraySizeException("size");
            }
            synchronized (buffer) {
                bufferSize = (size < 1) ? 1 : size;
                buffer.notifyAll();
            }
        }

        public void setBlocking(final boolean blocking) {
            synchronized (buffer) {
                this.blocking = blocking;
                buffer.notifyAll();
            }
        }

        public boolean getBlocking() {
            return blocking;
        }

        private final class DiscardSummary {

            private LoggingEvent maxEvent;

            private int count;

            public DiscardSummary(final LoggingEvent event) {
                maxEvent = event;
                count = 1;
            }

            /**
             * @author: xiaolingbao
             * @date: 2022/5/12 15:30
             * @param event
             * @description: 如果参数event的级别比maxEvent更高，则将maxEvent设为event
             */
            public void add(final LoggingEvent event) {
                if (event.getLevel().toInt() > maxEvent.getLevel().toInt()) {
                    maxEvent = event;
                }
                count++;
            }

            public LoggingEvent createEvent() {
                String msg = MessageFormat.format("Discarded {0} messages due to full event buffer including: {1}",
                        count, maxEvent.getMessage());
                return new LoggingEvent("AsyncAppender.DONT_REPORT_LOCATION",
                        Logger.getLogger(maxEvent.getLoggerName()),
                        maxEvent.getLevel(),
                        msg,
                        null);
            }

        }

        private class Dispatcher implements Runnable {

            private final AsyncAppender parent;

            private final List<LoggingEvent> buffer;

            private final Map<String, DiscardSummary> discardMap;

            private final AppenderPipelineImpl appenderPipeline;

            public Dispatcher(final AsyncAppender parent, final List<LoggingEvent> buffer,
                              final Map<String, DiscardSummary> discardMap, final AppenderPipelineImpl appenderPipeline) {
                this.parent = parent;
                this.buffer = buffer;
                this.appenderPipeline = appenderPipeline;
                this.discardMap = discardMap;
            }

            /**
             * @author: xiaolingbao
             * @date: 2022/5/12 17:02
             * @description: 死循环，一直在取出buffer中的LoggingEvent，并调用appenderPipeline进行处理
             */
            @Override
            public void run() {
                boolean isActive = true;

                try {
                    while (isActive) {
                        LoggingEvent[] events = null;

                        synchronized (buffer) {
                            int bufferSize = buffer.size();
                            isActive = !parent.closed;

                            while ((bufferSize == 0) && isActive) {
                                buffer.wait();
                                bufferSize = buffer.size();
                                isActive = !parent.closed;
                            }

                            if (bufferSize > 0) {
                                events = new LoggingEvent[bufferSize + discardMap.size()];
                                buffer.toArray(events);

                                int index = bufferSize;
                                Collection<DiscardSummary> discardSummaryCollection = discardMap.values();
                                for (DiscardSummary discardSummary : discardSummaryCollection) {
                                    events[index++] = discardSummary.createEvent();
                                }

                                buffer.clear();
                                discardMap.clear();
                                buffer.notifyAll();
                            }
                        }
                        if (events != null) {
                            for (LoggingEvent event : events) {
                                synchronized (appenderPipeline) {
                                    appenderPipeline.appendLoopOnAppenders(event);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }


    }

    private static class QuietWriter extends FilterWriter {
        protected Appender appender;

        public QuietWriter(Writer writer, Appender appender) {
            super(writer);
            this.appender = appender;
        }

        @Override
        public void write(String str) {
            if (str != null) {
                try {
                    out.write(str);
                } catch (Exception e) {
                    appender.handleError("write [" + str + "]失败", e, Appender.CODE_WRITE_FAILURE);
                }
            }
        }


        @Override
        public void flush() {
            try {
                out.flush();
            } catch (Exception e) {
                appender.handleError("flush writer失败", e, Appender.CODE_FLUSH_FAILURE);
            }
        }


    }

    public static class WriterAppender extends Appender {

        protected boolean immediateFlush = true;

        private String encoding;

        protected QuietWriter quietWriter;

        public WriterAppender() {

        }

        @Override
        public void activateOptions() {

        }

        public void setImmediateFlush(boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
        }

        public boolean getImmediateFlush() {
            return immediateFlush;
        }

        @Override
        public void append(LoggingEvent event) {
            if (!checkEntryConditions()) {
                return;
            }
            kernelAppend(event);
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 20:51
         * @param event
         * @description: 该方法是最后真正执行append操作的方法，参数为日志事件event
         */
        protected void kernelAppend(LoggingEvent event) {
            this.quietWriter.write(this.layout.format(event));
            if (!layout.ignoreThrowable()) {
                String[] throwableStr = event.getThrowableStr();
                if (throwableStr != null) {
                    for (String s : throwableStr) {
                        this.quietWriter.write(s);
                        this.quietWriter.write(LINE_SEP);
                    }
                }
            }

            if (shouldFlush(event)) {
                this.quietWriter.flush();
            }

        }

        private boolean shouldFlush(final LoggingEvent event) {
            return event != null && immediateFlush;
        }

        private boolean checkEntryConditions() {
            if (this.closed) {
                SysLogger.warn("该appender已经关闭,不能进行写操作");
                return false;
            }

            if (this.quietWriter == null) {
                handleError("名为[" + name + "]的appender没有设置输出流或file");
                return false;
            }

            if (this.layout == null) {
                handleError("名为[" + name + "]的appender没有设置layout");
                return false;
            }

            return true;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/11 20:55
         * @description: 关闭WriterAppender,write最后的footer，然后将quiteWrite关闭并置空
         */
        @Override
        public synchronized void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            writeFooter();
            reset();
        }

        private void writeFooter() {
            if (layout != null) {
                String footer = layout.getFooter();
                if (footer != null && this.quietWriter != null) {
                    this.quietWriter.write(footer);
                    this.quietWriter.flush();
                }
            }
        }

        protected void writeHeader() {
            if (layout != null) {
                String header = layout.getHeader();
                if (header != null && this.quietWriter != null) {
                    this.quietWriter.write(header);
                }
            }
        }

        private void reset() {
            closeWriter();
            this.quietWriter = null;
        }

        private void closeWriter() {
            if (quietWriter != null) {
                try {
                    quietWriter.close();
                } catch (IOException e) {
                    handleError("无法关闭 quiteWriter " + quietWriter, e, CODE_CLOSE_FAILURE);
                }
            }
        }

        protected OutputStreamWriter createWriter(OutputStream outputStream) {
            OutputStreamWriter retOutputStreamWriter = null;
            String encoding = getEncoding();
            if (encoding != null) {
                try {
                    retOutputStreamWriter = new OutputStreamWriter(outputStream, encoding);
                } catch (IOException e) {
                    SysLogger.warn("初始化output writer错误,可能原因是设置了错误的encoding");
                }
            }
            if (retOutputStreamWriter == null) {
                retOutputStreamWriter = new OutputStreamWriter(outputStream);
            }
            return retOutputStreamWriter;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String value) {
            encoding = value;
        }

        public synchronized void setWriter(Writer writer) {
            reset();
            this.quietWriter = new QuietWriter(writer, this);
            writeHeader();
        }



    }

    public static class FileAppender extends WriterAppender {

        private boolean fileAppend = true;

        protected String fileName = null;

        protected boolean bufferedIO = false;

        protected int bufferSize = 8 * 1024;



        public FileAppender() {

        }

        public FileAppender(Layout layout, String filename, boolean fileAppend) throws IOException {
            this.layout = layout;
            this.setFile(filename, fileAppend, false, bufferSize);
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/12 11:10
         * @param fileName
         * @param fileAppend 是否以追加方式写文件
         * @param bufferedIO 是否采用BufferedWriter
         * @param bufferSize
         * @description: 创建QuietWrite，并设其输出流为对应文件的输出流.
         *
         *               调用此方法，最开始会重置QuietWrite和File相关信息，
         *               然后会创建对应文件的fileOutputStream，若对应文件不存在,
         *               则创建其父目录的fileOutputStream,然后设其为writer的输出流
         *               最后将FileAppender的属性设为参数中的值
         */
        public synchronized void setFile(String fileName, boolean fileAppend, boolean bufferedIO, int bufferSize) throws IOException {
            SysLogger.debug("setFile方法被调用,文件名为:" + fileName + ", fileAppend设为" + fileAppend);
            if (bufferedIO) {
                setImmediateFlush(false);
            }

            reset();
            FileOutputStream fileOutputStream;
            try {
                fileOutputStream = new FileOutputStream(fileName, fileAppend);
            } catch (FileNotFoundException e) {
                String parentName = new File(fileName).getParent();
                if (parentName != null) {
                    File parentDir = new File(parentName);
                    if (!parentDir.exists() && parentDir.mkdirs()) {
                        fileOutputStream = new FileOutputStream(fileName, fileAppend);
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }

            Writer fileWriter = createWriter(fileOutputStream);
            if (bufferedIO) {
                fileWriter = new BufferedWriter(fileWriter, bufferSize);
            }
            this.setQuietWriterForFiles(fileWriter);
            this.fileName = fileName;
            this.fileAppend = fileAppend;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            writeHeader();
            SysLogger.debug("setFile 成功结束");

        }

        protected void setQuietWriterForFiles(Writer writer) {
            this.quietWriter = new QuietWriter(writer, this);
        }

        public void setFile(String file) {
            fileName = file.trim();
        }

        private void reset() {
            closeFile();
            this.fileName = null;
            super.reset();
        }

        protected void closeFile() {
            if (this.quietWriter != null) {
                try {
                    this.quietWriter.close();
                } catch (IOException e) {
                    if (e instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                    }
                    SysLogger.error("无法关闭 " + quietWriter, e);
                }
            }
        }

        public boolean getFileAppend() {
            return fileAppend;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public void activateOptions() {
            if (fileName != null) {
                try {
                    setFile(fileName, fileAppend, bufferedIO, bufferSize);
                } catch (IOException e) {
                    handleError("setFile失败,fileName为: " + fileName + ",fileAppend为: " + fileAppend, e, CODE_FILE_OPEN_FAILURE);
                }
            } else {
                SysLogger.warn("名为[" + name + "]的appender设置file option失败");
            }
        }

        public boolean getBufferedIO() {
            return this.bufferedIO;
        }

        public int getBufferSize() {
            return this.bufferSize;
        }

        public void setFileAppend(boolean fileAppend) {
            this.fileAppend = fileAppend;
        }

        public void setBufferedIO(boolean bufferedIO) {
            this.bufferedIO = bufferedIO;
            if (bufferedIO) {
                immediateFlush = false;
            }
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/14 16:43
     * @description: RollingFile的意思是指当一个日志文件大小达到最大值时，则创建新的日志文件，
     *               防止单个日志文件大小过大
     */
    public static class RollingFileAppender extends FileAppender {
        // 单个日志文件大小最多为10M
        protected long maxFileSize = 10 * 1024 * 1024;

        protected int maxBackupIndex = 1;

        private long nextRollover = 0;

        public RollingFileAppender() {
            super();
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public int getMaxBackupIndex() {
            return maxBackupIndex;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/14 17:17
         * @description: 与文件重命名相关，当一个文件写满了时，需要创建新日志文件
         */
        public void rollOver() {
            File target;
            File file;

            if (quietWriter != null) {
                long size = ((CountingQuietWriter) quietWriter).getCount();
                SysLogger.debug("rolling over count=" + size);
                nextRollover = size + maxFileSize;
            }
            SysLogger.debug("maxBackupIndex=" + maxBackupIndex);

            boolean renameSucceeded = true;
            if (maxBackupIndex > 0) {
                file = new File(fileName + '.' + maxBackupIndex);
                if (file.exists()) {
                    renameSucceeded = file.delete();
                }

                for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
                    file = new File(fileName + "." + i);
                    if (file.exists()) {
                        target = new File(fileName + '.' + (i + 1));
                        SysLogger.debug("将file " + file + "重命名为 " + target);
                        renameSucceeded = file.renameTo(target);
                    }
                }

                if (renameSucceeded) {
                    target = new File(fileName + "." + 1);
                    this.closeFile();
                    file = new File(fileName);
                    SysLogger.debug("将file " + file + "重命名为 " + target);
                    renameSucceeded = file.renameTo(target);

                    if (!renameSucceeded) {
                        try {
                            this.setFile(fileName, true, bufferedIO, bufferSize);
                        } catch (IOException e) {
                            if (e instanceof InterruptedIOException) {
                                Thread.currentThread().interrupt();
                            }
                            SysLogger.error("setFile(" + fileName + ", true) 方法调用失败", e);
                        }
                    }
                }
            }
            if (renameSucceeded) {
                try {
                    this.setFile(fileName, false, bufferedIO, bufferSize);
                    nextRollover = 0;
                } catch (IOException e) {
                    if (e instanceof InterruptedIOException) {
                        Thread.currentThread().interrupt();
                    }
                    SysLogger.error("setFile(" + fileName + ", true) 方法调用失败", e);
                }
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/14 17:20
         * @param fileName
         * @param fileAppend
         * @param bufferedIO
         * @param bufferSize
         * @description: 当启动fileAppend方式时，将该file的大小赋到CountingQuietWriter的count中
         */
        @Override
        public synchronized void setFile(String fileName, boolean fileAppend, boolean bufferedIO, int bufferSize) throws IOException {
            super.setFile(fileName, fileAppend, this.bufferedIO, this.bufferSize);
            if (fileAppend) {
                File file = new File(fileName);
                ((CountingQuietWriter) quietWriter).setCount(file.length());
            }
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public void setMaxBackupIndex(int maxBackupIndex) {
            this.maxBackupIndex = maxBackupIndex;
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/14 17:22
         * @param writer
         * @description: 将quietWriter设为CountingQuietWriter
         */
        @Override
        protected void setQuietWriterForFiles(Writer writer) {
            this.quietWriter = new CountingQuietWriter(writer, this);
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/14 17:26
         * @param event
         * @description: 当CountingQuietWrite总计write的字符超过文件最大大小时，执行rollOver
         */
        @Override
        protected void kernelAppend(LoggingEvent event) {
            super.kernelAppend(event);
            if (fileName != null && quietWriter != null) {
                long size = ((CountingQuietWriter)quietWriter).getCount();
                if (size >= maxFileSize && size >= nextRollover) {
                    rollOver();
                }
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/14 16:51
         * @description: 基于QuietWriter封装了统计总共write了多少字符的功能
         */
        protected class CountingQuietWriter extends QuietWriter {
            protected long count;

            public CountingQuietWriter(Writer writer, Appender appender) {
                super(writer, appender);
            }

            @Override
            public void write(String str) {
                try {
                    out.write(str);
                    count += str.length();
                } catch (IOException e) {
                    appender.handleError("write写错误", e, Appender.CODE_WRITE_FAILURE);
                }
            }

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }
        }
    }

    public static class ConsoleAppender extends WriterAppender {

        private String target = SYSTEM_OUT;

        public ConsoleAppender() {

        }

        public void setTarget(String target) {
            String t = target.trim();
            if (SYSTEM_OUT.equalsIgnoreCase(t)) {
                this.target = SYSTEM_OUT;
            } else if (SYSTEM_ERR.equalsIgnoreCase(t)) {
                this.target = SYSTEM_ERR;
            } else {
                targetWarn(target);
            }
        }

        public String getTarget() {
            return target;
        }

        private void targetWarn(String target) {
            SysLogger.warn("[" + target + "] 应为System.out或System.err");
        }

        @Override
        public void activateOptions() {
            if (target.equals(SYSTEM_ERR)) {
                setWriter(createWriter(System.err));
            } else {
                setWriter(createWriter(System.out));
            }
            super.activateOptions();
        }

        protected final void closeWriter() {

        }

    }

    public static LayoutBuilder newLayoutBuilder() {
        return new LayoutBuilder();
    }

    public static class LayoutBuilder {
        private Layout layout;

        public LayoutBuilder withSimpleLayout() {
            layout = new SimpleLayout();
            return this;
        }

        public LayoutBuilder withDefaultLayout() {
            layout = new DefaultLayout();
            return this;
        }

        public Layout build() {
            if (layout == null) {
                layout = new SimpleLayout();
            }
            return layout;
        }
    }

    public static class SimpleLayout extends Layout {

        @Override
        public String format(LoggingEvent event) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(event.getLevel().toString());
            stringBuilder.append(" - ");
            stringBuilder.append(event.getRenderedMessage());
            stringBuilder.append("\r\n");
            return stringBuilder.toString();
        }

        @Override
        public boolean ignoreThrowable() {
            return true;
        }

    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/11 20:36
     * @description: 默认的日志输出格式: %d{yyy-MM-dd HH:mm:ss,SSS} %p %c{1}%L - %m%n
     */
    public static class DefaultLayout extends Layout {

        private static final SimpleDateFormat simpleDateFormat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

        @Override
        public String format(LoggingEvent event) {
            StringBuilder stringBuilder = new StringBuilder();
            String formatDate = simpleDateFormat.format(new Date(event.timeStamp));
            stringBuilder.append(formatDate)
                        .append(" ")
                        .append(event.getLevel())
                        .append(" ")
                        .append(event.getLoggerName())
                        .append(" - ")
                        .append(event.getRenderedMessage());
            String[] throwableStr = event.getThrowableStr();
            if (throwableStr != null) {
                stringBuilder.append("\r\n");
                for (String s : throwableStr) {
                    stringBuilder.append(s);
                    stringBuilder.append("\r\n");
                }
            }
            stringBuilder.append("\r\n");

            return stringBuilder.toString();
        }

        @Override
        public boolean ignoreThrowable() {
            return true;
        }
    }
}
