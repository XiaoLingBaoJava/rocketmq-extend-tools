package com.xiaolingbao.logging;

import com.xiaolingbao.logging.inner.Logger;
import org.apache.rocketmq.logging.InnerLoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/10 15:01
 * @description: 
 */
public class InnerLogFactory extends LogFactory {

    // 在构造函数中进行日志的注册，将自身注册进HashMap
    public InnerLogFactory() {
        doRegister();
    }

    @Override
    public void shutdown() {
        Logger.getRepository().shutdown();
    }

    @Override
    public Log getLoggerInstance(String name) {
        return new InnerLog(name);
    }

    @Override
    public String getLoggerType() {
        if (ClientLoggerConfig.isUseFileLog()) {
            return LOGGER_FILE;
        } else {
            return LOGGER_CMD;
        }
    }

    // 内部类InnerLog,实现了Log接口
    public static class InnerLog implements Log {

        // 封装了自定义的Logger
        private Logger logger;

        public InnerLog(String name) {
            this.logger = Logger.getLogger(name);
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public void debug(String var1) {
            logger.debug(var1);
        }

        @Override
        public void debug(String var1, Throwable var2) {
            logger.debug(var1, var2);
        }

        @Override
        public void info(String var1) {
            logger.info(var1);
        }

        @Override
        public void info(String var1, Throwable var2) {
            logger.info(var1, var2);
        }

        @Override
        public void warn(String var1) {
            logger.warn(var1);
        }

        @Override
        public void warn(String var1, Throwable var2) {
            logger.warn(var1, var2);
        }

        @Override
        public void error(String var1) {
            logger.error(var1);
        }

        @Override
        public void error(String var1, Throwable var2) {
            logger.error(var1, var2);
        }

        @Override
        public void debug(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void debug(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void debug(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            logger.error(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            logger.error(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            logger.error(format.getMessage(), format.getThrowable());
        }

        public Logger getLogger() {
            return logger;
        }
    }

    /**
     * @author: xiaolingbao
     * @date: 2022/5/13 10:57
     * @description: 用户在log的时候需要传入message、throwable、argArray三部分,
     *               FormattingTuple封装了这三部分，其中argArray就是占位符对应的参数值数组
     */
    public static class FormattingTuple {
        private String message;
        private Throwable throwable;
        private Object[] argArray;

        public FormattingTuple(String message) {
            this(message, null, null);
        }

        public FormattingTuple(String message, Object[] argArray, Throwable throwable) {
            this.message = message;
            this.throwable = throwable;
            if (throwable == null) {
                this.argArray = argArray;
            } else {
                this.argArray = trimmedCopy(argArray);
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/13 9:34
         * @param argArray
         * @return java.lang.Object[]
         * @description: 返回不含argArray中最后一个元素的copy
         */
        private static Object[] trimmedCopy(Object[] argArray) {
            if (argArray != null && argArray.length != 0) {
                int trimmedLen = argArray.length - 1;
                Object[] trimmed = new Object[trimmedLen];
                System.arraycopy(argArray, 0, trimmed, 0, trimmedLen);
                return trimmed;
            } else {
                throw new IllegalStateException("argArray为null或为空");
            }
        }

        public String getMessage() {
            return this.message;
        }

        public Object[] getArgArray() {
            return this.argArray;
        }

        public Throwable getThrowable() {
            return this.throwable;
        }

    }

    public static class MessageFormatter {
        public MessageFormatter() {

        }

        public static FormattingTuple format(String messagePattern, Object arg) {
            return arrayFormat(messagePattern, new Object[]{arg});
        }
        
        public static FormattingTuple format(String messagePattern, Object arg1, Object arg2) {
            return arrayFormat(messagePattern, new Object[]{arg1, arg2});
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/13 9:59
         * @param argArray
         * @return java.lang.Throwable
         * @description: 若argArray数组的最后一个元素为Throwable,则将其返回，否则返回null
         */
        private static Throwable getThrowableCandidate(Object[] argArray) {
            if (argArray != null && argArray.length != 0) {
                Object lastEntry = argArray[argArray.length - 1];
                return lastEntry instanceof Throwable ? (Throwable) lastEntry : null;
            } else {
                return null;
            }
        }

        public static FormattingTuple arrayFormat(String messagePattern, Object[] argArray) {
            Throwable throwableCandidate = getThrowableCandidate(argArray);
            if (messagePattern == null) {
                return new FormattingTuple(null, argArray, throwableCandidate);
            } else if (argArray == null) {
                return new FormattingTuple(messagePattern);
            } else {
                int i = 0;
                StringBuilder stringBuilder = new StringBuilder(messagePattern.length() + 50);
                int len;
                for (len = 0; len < argArray.length; ++len) {
                    int j = messagePattern.indexOf("{}", i);
                    if (j == -1) {
                        if (i == 0) {
                            // 若没有使用占位符
                            return new FormattingTuple(messagePattern, argArray, throwableCandidate);
                        }
                        stringBuilder.append(messagePattern.substring(i));
                        return new FormattingTuple(stringBuilder.toString(), argArray, throwableCandidate);
                    }

                    if (isEscapeDelimeter(messagePattern, j)) {
                        if (!isDoubleEscaped(messagePattern, j)) {
                            // 若{}前有'/'
                            --len;
                            stringBuilder.append(messagePattern.substring(i, j - 1));
                            stringBuilder.append('{');
                            i = j + 1;
                        } else {
                            // 若{}前有'//'
                            stringBuilder.append(messagePattern.substring(i, j - 1));
                            deeplyAppendParameter(stringBuilder, argArray[len], null);
                            i = j + 2;
                        }
                    } else {
                        stringBuilder.append(messagePattern.substring(i, j));
                        deeplyAppendParameter(stringBuilder, argArray[len], null);
                        i = j + 2;
                    }
                }

                stringBuilder.append(messagePattern.substring(i));
                if (len < argArray.length - 1) {
                    return new FormattingTuple(stringBuilder.toString(), argArray, throwableCandidate);
                } else {
                    return new FormattingTuple(stringBuilder.toString(), argArray, null);
                }
            }
        }

        private static void deeplyAppendParameter(StringBuilder stringBuilder, Object o, Map<Object[], Object> seenMap) {
            if (o == null) {
                stringBuilder.append("null");
            } else {
                if (!o.getClass().isArray()) {
                    safeObjectAppend(stringBuilder, o);
                } else if (o instanceof boolean[]) {
                    booleanArrayAppend(stringBuilder, (boolean[]) o);
                } else if (o instanceof byte[]) {
                    byteArrayAppend(stringBuilder, (byte[]) o);
                } else if (o instanceof char[]) {
                    charArrayAppend(stringBuilder, (char[]) o);
                } else if (o instanceof short[]) {
                    shortArrayAppend(stringBuilder, (short[]) o);
                } else if (o instanceof int[]) {
                    intArrayAppend(stringBuilder, (int[]) o);
                } else if (o instanceof long[]) {
                    longArrayAppend(stringBuilder, (long[]) o);
                } else if (o instanceof float[]) {
                    floatArrayAppend(stringBuilder, (float[]) o);
                } else if (o instanceof double[]) {
                    doubleArrayAppend(stringBuilder, (double[]) o);
                } else {
                    objectArrayAppend(stringBuilder, (Object[]) o, seenMap);
                }
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/13 16:39
         * @param stringBuilder
         * @param o
         * @description: 往stringBuilder append Object时加了异常处理，防止toString()方法抛出异常
         */
        private static void safeObjectAppend(StringBuilder stringBuilder, Object o) {
            try {
                String str = o.toString();
                stringBuilder.append(str);
            } catch (Throwable throwable) {
                System.err.println("RokcetmqExtendTool logger在对对象[" + o.getClass().getName() + "]调用toString方法时失败");
                throwable.printStackTrace();
                stringBuilder.append("[FAILED toString()]");
            }
        }

        private static void objectArrayAppend(StringBuilder stringBuilder, Object[] array, Map<Object[], Object> seenMap) {
            if (seenMap == null) {
                seenMap = new HashMap<>();
            }
            stringBuilder.append('[');
            if (!seenMap.containsKey(array)) {
                seenMap.put(array, null);
                int len = array.length;

                for (int i = 0; i < len; i++) {
                    deeplyAppendParameter(stringBuilder, array[i], seenMap);
                    if (i != len - 1) {
                        stringBuilder.append(", ");
                    }
                }

                seenMap.remove(array);
            } else {
                stringBuilder.append("...");
            }

            stringBuilder.append(']');
        }

        private static void booleanArrayAppend(StringBuilder stringBuilder, boolean[] booleanArray) {
            stringBuilder.append('[');
            int len = booleanArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(booleanArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }

        private static void byteArrayAppend(StringBuilder stringBuilder, byte[] byteArray) {
            stringBuilder.append('[');
            int len = byteArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(byteArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }

        private static void charArrayAppend(StringBuilder stringBuilder, char[] charArray) {
            stringBuilder.append('[');
            int len = charArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(charArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }
        
        private static void shortArrayAppend(StringBuilder stringBuilder, short[] shortArray) {
            stringBuilder.append('[');
            int len = shortArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(shortArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }
        
        private static void intArrayAppend(StringBuilder stringBuilder, int[] intArray) {
            stringBuilder.append('[');
            int len = intArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(intArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }
        
        private static void longArrayAppend(StringBuilder stringBuilder, long[] longArray) {
            stringBuilder.append('[');
            int len = longArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(longArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }
        
        private static void floatArrayAppend(StringBuilder stringBuilder, float[] floatArrray) {
            stringBuilder.append('[');
            int len = floatArrray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(floatArrray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }
        
        private static void doubleArrayAppend(StringBuilder stringBuilder, double[] doubleArray) {
            stringBuilder.append('[');
            int len = doubleArray.length;
            for (int i = 0; i < len; i++) {
                stringBuilder.append(doubleArray[i]);
                if (i != len - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(']');
        }


        /**
         * @author: xiaolingbao
         * @date: 2022/5/13 16:18
         * @param messagePattern
         * @param delimeterStartIndex
         * @return boolean
         * @description: 判断{}前是否有\
         */
        private static boolean isEscapeDelimeter(String messagePattern, int delimeterStartIndex) {
            if (delimeterStartIndex == 0) {
                return false;
            } else {
                char potentialEscape = messagePattern.charAt(delimeterStartIndex - 1);
                // 92为字符'\'
                return potentialEscape == 92;
            }
        }

        /**
         * @author: xiaolingbao
         * @date: 2022/5/13 16:26
         * @param messagePattern
         * @param delimeterStartIndex
         * @return boolean
         * @description: 判断{}前是否有\\
         */
        private static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
            return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == 92;
        }
    }
}
