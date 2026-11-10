package org.lambda.framework.rsocket.adapter;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;

import static org.lambda.framework.common.exception.basic.GlobalException.EX_PRIEX;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.*;

public class RsocketGlobalExceptionHandler  {
    public  Throwable handleException(Throwable e) {
        GlobalException globalException;
        if (e instanceof GlobalException original) {
            globalException = original;
        }else if (e instanceof MethodArgumentResolutionException) {
            // 如果是方法参数解析异常，可能是客户端请求参数错误导致的，返回参数错误的信息
            globalException =   new EventException(ES_RSOCKET_001);
        }else if (e instanceof InvalidFormatException) {
            // 如果是方法参数解析异常，可能是客户端请求参数错误导致的，返回参数错误的信息
            globalException =   new EventException(ES_RSOCKET_002);
        } else {
            String message = StringUtils.defaultIfBlank(e.getMessage(), ES_RSOCKET_000.getMessage());
            int separator = message.indexOf(EX_PRIEX);
            globalException = separator < 0
                    ? new GlobalException(ES_RSOCKET_000.getCode(), message)
                    : new GlobalException(message.substring(0, separator), message.substring(separator + EX_PRIEX.length()));
        }
        return new RuntimeException(globalException.getSerializeMessage(), e);
    }
}
