package org.lambda.framework.common.util.sample;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/** @deprecated Use {@link org.lambda.framework.common.util.JsonUtil}. */
@Deprecated
public final class JsonUtil {
    public static ObjectMapper getJsonFactory() {
        return org.lambda.framework.common.util.JsonUtil.mapper();
    }

    public static String objToString(Object data) {
        return org.lambda.framework.common.util.JsonUtil.write(data);
    }

    public static <T> Optional<T> mapToObj(Map<?, ?> data, Class<T> type) {
        return org.lambda.framework.common.util.JsonUtil.fromMap(data, type);
    }

    public static <T> Optional<T> stringToObj(String data, Class<T> type) {
        return org.lambda.framework.common.util.JsonUtil.read(data, type);
    }
}
