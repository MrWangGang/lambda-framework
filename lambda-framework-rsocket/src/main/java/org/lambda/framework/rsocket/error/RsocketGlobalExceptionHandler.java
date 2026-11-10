package org.lambda.framework.rsocket.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.validation.method.MethodValidationException;

import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.*;

public class RsocketGlobalExceptionHandler  {
    public  Throwable handleException(Throwable e) {
        GlobalException globalException;
        if (e instanceof GlobalException original) {
            globalException = original;
        } else if (e instanceof MethodArgumentResolutionException
                || e instanceof ConstraintViolationException || e instanceof MethodValidationException) {
            globalException = new EventException(ES_RSOCKET_001);
        }else if (e instanceof InvalidFormatException) {
            // 如果是方法参数解析异常，可能是客户端请求参数错误导致的，返回参数错误的信息
            globalException =   new EventException(ES_RSOCKET_002);
        } else {
            globalException = GlobalException.fromSerialized(e.getMessage())
                    .orElseGet(() -> new EventException(ES_RSOCKET_000));
        }
        return new RuntimeException(globalException.getSerializeMessage(), e);
    }
}
