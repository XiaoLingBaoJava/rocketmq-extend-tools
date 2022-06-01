package com.xiaolingbao.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Strings;
import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * @author: xiaolingbao
 * @date: 2022/5/17 8:41
 * @description: 
 */
public class JsonUtil {
    private static final Log log = ClientLogger.getLog();
    private static ObjectMapper objectMapper = new ObjectMapper();


    private JsonUtil() {

    }

    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setFilters(new SimpleFilterProvider().setFailOnUnknownId(false));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public static void writeValue(Writer writer, Object obj) {
        try {
            objectMapper.writeValue(writer, obj);
        } catch (IOException e) {
            log.error("发生写异常, write obj: {}", obj.toString(), e);
        }
    }

    public static <T> String obj2String(T src) {
        if (src == null) {
            return null;
        }
        try {
            return src instanceof String ? (String)src : objectMapper.writeValueAsString(src);
        } catch (Exception e) {
            log.error("将object解析为String失败, object source: {}", src, e);
            return null;
        }
    }

    public static <T> byte[] obj2Byte(T src) {
        if (src == null) {
            return null;
        }
        try {
            return src instanceof byte[] ? (byte[])src : objectMapper.writeValueAsBytes(src);
        } catch (Exception e) {
            log.error("将object解析为byte[]失败, object source: {}", src, e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, Class<T> clazz) {
        if (Strings.isNullOrEmpty(str) || clazz == null) {
            log.warn("string2Obj方法调用有参数为空, str: {}, clazz: {}", str, clazz);
            return null;
        }
        str = escapesSpecialChar(str);
        try {
            return clazz.equals(String.class) ? (T)str : objectMapper.readValue(str, clazz);
        } catch (Exception e) {
            log.error("将String解析为Object失败, String: {}, Class: {}", str, clazz, e);
            return null;
        }
    }

    public static <T> T bytes2Obj(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            log.warn("bytes2Obj方法调用有参数为空, bytes: {}, clazz: {}", bytes, clazz);
            return null;
        }
        try {
            return clazz.equals(byte[].class) ? (T)bytes : objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            log.error("将byte[]解析为object失败, byte[]: {}, class: {}", Arrays.toString(bytes), clazz, e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, TypeReference<T> typeReference) {
        if (Strings.isNullOrEmpty(str) || typeReference == null) {
            log.warn("string2Obj方法调用有参数为空, str: {}, typeReference: {}", str, typeReference);
            return null;
        }
        str = escapesSpecialChar(str);
        try {
            return (T)(typeReference.getType().equals(String.class) ? str : objectMapper.readValue(str, typeReference));
        } catch (Exception e) {
            log.error("Parse String to Object error\nString: {}\nTypeReference<T>: {}\nError: {}", str,
                    typeReference.getType(), e);
            return null;
        }
    }

    private static String escapesSpecialChar(String str) {
        return str.replace("\n", "\\n").replace("\r", "\\r");
    }

}
