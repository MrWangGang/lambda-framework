/*
package org.lambda.framework.rsocket.adapter;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_000;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_001;

@Component
public class RsocketGlobalExceptionHandler implements ErrorWebExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(RsocketGlobalExceptionHandler.class);

    @MessageExceptionHandler(Throwable.class)
    public void handleException(Throwable e) {
        this.logger.error("RsocketGlobalExceptionHandler", e);
        // 处理异常并返回适当的响应
        if(e instanceof GlobalException){
            throw (GlobalException) e;
        }
        if (e instanceof MethodArgumentResolutionException) {
            // 如果是方法参数解析异常，可能是客户端请求参数错误导致的，返回参数错误的信息
            throw new EventException(ES_RSOCKET_001);
        }
        throw new GlobalException(ES_RSOCKET_000.getCode(), StringUtils.isBlank(e.getMessage())? ES_RSOCKET_000.getMessage():e.getMessage());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        return null;
    }
}
*/
