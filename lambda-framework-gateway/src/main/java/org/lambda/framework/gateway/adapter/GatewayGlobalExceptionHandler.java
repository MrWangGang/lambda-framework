package org.lambda.framework.gateway.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.rsocket.exceptions.ConnectionErrorException;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.lambda.framework.common.exception.basic.GlobalException.EX_PRIEX;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Configuration
@Order(-2)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

    public static final String INTERACTION_ATTRIBUTE = GatewayGlobalExceptionHandler.class.getName() + ".interaction";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(throwable);
        }
        GatewayInteraction interaction = exchange.getAttribute(INTERACTION_ATTRIBUTE);
        boolean ndjson = interaction != null && interaction.isNdjson();
        return writeResponse(response, errorResponse(response, throwable,
                interaction != null && interaction != GatewayInteraction.REQUEST_RESPONSE), ndjson);
    }

    public static ResponseTemplete errorResponse(ServerHttpResponse response, Throwable throwable, boolean arrayData) {
        boolean upstreamUnavailable = throwable instanceof ConnectionErrorException || throwable instanceof ConnectException
                || throwable instanceof UnknownHostException || throwable instanceof ClosedChannelException
                || throwable instanceof CancellationException && "Pool is exhausted".equals(throwable.getMessage());
        if (!response.isCommitted()) {
            if (upstreamUnavailable) {
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            } else if (throwable instanceof ErrorResponse error) {
                response.setStatusCode(error.getStatusCode());
                response.getHeaders().putAll(error.getHeaders());
            } else if (throwable instanceof DataBufferLimitException) {
                response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            } else {
                response.setStatusCode(HttpStatus.OK);
            }
        }
        var result = upstreamUnavailable ? result(ES_GATEWAY_000.getCode(), "RSocket upstream unavailable")
                : handleTransferException(throwable);
        if (arrayData) {
            result.setData(List.of());
        }
        return result;
    }

    private static ResponseTemplete handleTransferException(Throwable e) {
        if (!(e instanceof ErrorResponse) && !(e instanceof DataBufferLimitException)) {
            logger.error("Gateway request failed", e);
        }
        //抛出内部的错误
        if(e instanceof GlobalException) {
            GlobalException globalException = (GlobalException)e;
            return result(globalException.getCode(), globalException.getMessage());
        }

        if(StringUtils.isBlank(e.getMessage())){
            return result(ES_GATEWAY_000.getCode(), ES_GATEWAY_000.getMessage());
        }
        // 找到第一个冒号的位置
        int index = e.getMessage().indexOf(EX_PRIEX);
        if (index != -1) {
            // 获取第一个冒号前面的字符串
            String firstPart = e.getMessage().substring(0, index);
            // 获取第一个冒号后面的字符串
            String secondPart = e.getMessage().substring(index + EX_PRIEX.length());
            return result(firstPart,secondPart);
        } else {
            // 如果没有找到冒号，则输出原始错误信息
            return result(ES_GATEWAY_000.getCode(), StringUtils.isBlank(e.getMessage())?ES_GATEWAY_000.getMessage():e.getMessage());
        }
    }

    private static ResponseTemplete result(String code, String message){
        ResponseTemplete responseTemplete = new ResponseTemplete();
        responseTemplete.setServiceCode(code);
        responseTemplete.setServiceMessage(message);
        return responseTemplete;
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, ResponseTemplete errorBody, boolean ndjson) {
        response.getHeaders().setContentType(ndjson ? MediaType.APPLICATION_NDJSON : MediaType.APPLICATION_JSON);
        try {
            byte[] json = JsonUtil.getJsonFactory().writeValueAsBytes(errorBody);
            byte[] bytes = ndjson ? Arrays.copyOf(json, json.length + 1) : json;
            if (ndjson) {
                bytes[json.length] = '\n';
            }
            return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException error) {
            return Mono.error(error);
        }
    }
}
