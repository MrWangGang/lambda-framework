package org.lambda.framework.gateway.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;

import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;
import static org.lambda.framework.gateway.enums.GlobalResponseContentType.APPLICATION_JSON_UTF8;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Component
@Order(-2)
@Slf4j
public class WebGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(WebGlobalExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        return writeResponse(serverWebExchange,handleTransferException(throwable));
    }

    private ResponseTemplete handleTransferException(Throwable e) {
        logger.error("GlobalExceptionHandle",e);
        if(e instanceof GlobalException){
            return result(((GlobalException)e).getCode(),e.getMessage());
        }
        return result(ES_GATEWAY_000.getCode(), StringUtils.isBlank(e.getMessage())?ES_GATEWAY_000.getMessage():e.getMessage());
    }

    private ResponseTemplete result(String code, String message){
        ResponseTemplete responseTemplete = new ResponseTemplete();
        responseTemplete.setServiceCode(code);
        responseTemplete.setServiceMessage(message);
        return responseTemplete;
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ResponseTemplete errorBody) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().setContentType(APPLICATION_JSON_UTF8);
        DataBuffer dataBuffer = response.bufferFactory()
                .allocateBuffer().write(JsonUtil.objToString(errorBody).getBytes());
        return response.writeAndFlushWith(Mono.just(ByteBufMono.just(dataBuffer)));
    }





}
