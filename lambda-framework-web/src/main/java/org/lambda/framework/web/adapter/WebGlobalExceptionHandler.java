/*
package org.lambda.framework.web.adapter;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.lambda.framework.web.enums.WebExceptionEnum.ES_WEB_000;

@RestControllerAdvice
public class WebGlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(WebGlobalExceptionHandler.class);

    @ExceptionHandler(Throwable.class)
    public void handleException(Throwable e) {
        this.logger.error("WebGlobalExceptionHandler", e);
        // 处理异常并返回适当的响应
        if(e instanceof GlobalException){
            throw (GlobalException) e;
        }
        throw new GlobalException(ES_WEB_000.getCode(), StringUtils.isBlank(e.getMessage())? ES_WEB_000.getMessage():e.getMessage());
    }
}
*/
