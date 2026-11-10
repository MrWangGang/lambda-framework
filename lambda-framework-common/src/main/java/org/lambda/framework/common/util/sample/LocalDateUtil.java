package org.lambda.framework.common.util.sample;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.CommonExceptionEnum;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateUtil {
    public static LocalTime of(String time){
        if(StringUtils.isBlank(time))throw new EventException(CommonExceptionEnum.TIME_REQUIRED);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(time, formatter);
    }
}
