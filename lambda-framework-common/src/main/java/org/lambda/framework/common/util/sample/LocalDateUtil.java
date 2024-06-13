package org.lambda.framework.common.util.sample;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.basic.GlobalException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateUtil {
    public static LocalTime of(String time){
        if(StringUtils.isBlank(time))throw new GlobalException("ES_COMMON_000","time不能为空");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(time, formatter);
    }
}
