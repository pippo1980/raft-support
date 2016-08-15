package com.baidu.raft.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pippo on 16/7/27.
 */
public class JSONUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // OBJECT_MAPPER.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        // OBJECT_MAPPER.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        // OBJECT_MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    }

    public static String toString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBytes(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObject(byte[] result, Class<T> clazz) {
        if (result == null || result.length == 0) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(result, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("convert json:[%s] to object:[%s] due to error",
                    new String(result),
                    clazz), e);
        }
    }

    //    private static <T> T toObject(byte[] result, TypeReference<T> typeReference) {
    //        if (result == null || result.length == 0) {
    //            return null;
    //        }
    //
    //        try {
    //            return OBJECT_MAPPER.readValue(result, typeReference);
    //        } catch (Exception e) {
    //            throw new RuntimeException(String.format("convert json:[%s] to object:[%s] due to error",
    //                    new String(result),
    //                    typeReference), e);
    //        }
    //    }

    public static <T> T toObject(InputStream result, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(result, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObject(byte[] result, Assembler<T> assembler) {
        try {
            return assembler.assemble(OBJECT_MAPPER.readTree(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object[] toObject(final byte[] result, final Class<?>[] types) {
        return JSONUtil.toObject(result, new Assembler<Object[]>() {

            @Override
            public Object[] assemble(JsonNode jsonNode) {
                if (jsonNode == null) {
                    return ArrayUtils.EMPTY_OBJECT_ARRAY;
                }

                Validate.isTrue(jsonNode instanceof ArrayNode,
                        String.format("the message:[%s] can not cast to array", new String(result)));

                List<Object> result = new ArrayList<>();

                for (int i = 0;
                        i < types.length;
                        i++) {
                    Class<?> type = types[i];
                    JsonNode chlid = jsonNode.get(i);
                    try {
                        Object message = JSONUtil.OBJECT_MAPPER.treeToValue(chlid, type);
                        result.add(message);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                return result.toArray();
            }
        });
    }

    public interface Assembler<T> {

        T assemble(JsonNode jsonNode);

    }

}
