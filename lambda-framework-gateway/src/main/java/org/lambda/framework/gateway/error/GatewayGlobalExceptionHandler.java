package org.lambda.framework.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.rsocket.exceptions.ConnectionErrorException;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.enums.ExceptionEnumFunction;
import org.lambda.framework.enums.GatewayExceptionEnum;
import org.lambda.framework.enums.GatewayHttpExceptionEnum;
import org.lambda.framework.enums.LoadBalanceExceptionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpMethod;
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

import static org.lambda.framework.enums.GatewayExceptionEnum.*;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
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
                interaction != null && !interaction.isUnary()), ndjson);
    }

    public static ResponseTemplete errorResponse(ServerHttpResponse response, Throwable throwable, boolean arrayData) {
        var event = throwable instanceof EventException failure ? failure.getError() : null;
        boolean upstreamUnavailable = throwable instanceof ConnectionErrorException || throwable instanceof ConnectException
                || throwable instanceof UnknownHostException || throwable instanceof ClosedChannelException
                || throwable instanceof CancellationException && "Pool is exhausted".equals(throwable.getMessage())
                || event == LoadBalanceExceptionEnum.CONNECTION_UNAVAILABLE;
        if (!response.isCommitted()) {
            if (upstreamUnavailable) {
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            } else if (event instanceof GatewayHttpExceptionEnum http) {
                response.setStatusCode(HttpStatus.valueOf(http.getStatus()));
                if (http == GatewayHttpExceptionEnum.METHOD_NOT_ALLOWED) {
                    response.getHeaders().setAllow(java.util.Set.of(HttpMethod.POST));
                }
            } else if (throwable instanceof ErrorResponse error) {
                response.setStatusCode(error.getStatusCode());
                response.getHeaders().putAll(error.getHeaders());
            } else if (throwable instanceof DataBufferLimitException) {
                response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            } else {
                response.setStatusCode(HttpStatus.OK);
            }
        }
        var result = upstreamUnavailable ? result(UPSTREAM_UNAVAILABLE) : handleTransferException(throwable);
        if (arrayData) {
            result.setData(List.of());
        }
        return result;
    }

    private static ResponseTemplete handleTransferException(Throwable error) {
        if (error instanceof ErrorResponse http) {
            GatewayExceptionEnum code = switch (http.getStatusCode().value()) {
                case 400, 415, 422 -> INVALID_REQUEST;
                case 401, 403 -> NO_ACCESS;
                case 404 -> NOT_FOUND;
                case 405, 426 -> NOT_ALLOWED;
                case 413 -> CONTENT_TOO_LARGE;
                case 502 -> RESPONSE_MISSING;
                case 503 -> UPSTREAM_UNAVAILABLE;
                case 504 -> TIMEOUT;
                default -> UNEXPECTED_FAILURE;
            };
            return result(code);
        }
        if (error instanceof DataBufferLimitException) {
            return result(CONTENT_TOO_LARGE);
        }
        if (error instanceof GlobalException known) {
            return result(known.getCode(), known.getMessage());
        }
        return GlobalException.fromSerialized(error.getMessage())
                .map(known -> result(known.getCode(), known.getMessage()))
                .orElseGet(() -> {
                    logger.error("Gateway request failed", error);
                    return result(UNEXPECTED_FAILURE);
                });
    }

    private static ResponseTemplete result(ExceptionEnumFunction error) {
        return result(error.getCode(), error.getMessage());
    }

    private static ResponseTemplete result(String code, String message) {
        var response = new ResponseTemplete();
        response.setServiceCode(code);
        response.setServiceMessage(message);
        return response;
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, ResponseTemplete errorBody, boolean ndjson) {
        response.getHeaders().setContentType(ndjson ? MediaType.APPLICATION_NDJSON : MediaType.APPLICATION_JSON);
        try {
            byte[] json = JsonUtil.mapper().writeValueAsBytes(errorBody);
            byte[] bytes = ndjson ? Arrays.copyOf(json, json.length + 1) : json;
            if (ndjson) {
                bytes[json.length] = '\n';
            }
            return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException error) {
            return Mono.error(new EventException(UNEXPECTED_FAILURE, null, error));
        }
    }
}
