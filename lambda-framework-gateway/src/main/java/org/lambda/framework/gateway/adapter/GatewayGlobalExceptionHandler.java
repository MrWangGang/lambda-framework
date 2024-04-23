package org.lambda.framework.gateway.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;

import static org.lambda.framework.common.exception.basic.GlobalException.EX_PRIEX;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Configuration
@Order(-2)
@Slf4j
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {

        return writeResponse(serverWebExchange,handleTransferException(throwable));
    }



    private ResponseTemplete handleTransferException(Throwable e) {
        logger.error("GatewayGlobalExceptionHandler",e);
        if(StringUtils.isBlank(e.getMessage())){
            return result(ES_GATEWAY_000.getCode(), ES_GATEWAY_000.getMessage());
        }
        // 找到第一个冒号的位置
        int index = e.getMessage().indexOf(EX_PRIEX);
        if (index != -1) {
            // 获取第一个冒号前面的字符串
            String firstPart = e.getMessage().substring(0, index);
            // 获取第一个冒号后面的字符串
            String secondPart = e.getMessage().substring(index + 1);
            return result(firstPart,secondPart);
        } else {
            // 如果没有找到冒号，则输出原始错误信息
            return result(ES_GATEWAY_000.getCode(), StringUtils.isBlank(e.getMessage())?ES_GATEWAY_000.getMessage():e.getMessage());
        }
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
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        DataBuffer dataBuffer = response.bufferFactory()
                .allocateBuffer().write(JsonUtil.objToString(errorBody).getBytes());
        return response.writeAndFlushWith(Mono.just(ByteBufMono.just(dataBuffer)));
    }
}
