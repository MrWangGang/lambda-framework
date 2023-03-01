package org.lamb.framework.web.core.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.lamb.framework.common.enums.LambExceptionEnum;
import org.lamb.framework.common.exception.basic.LambGlobalException;
import org.lamb.framework.common.templete.LambResponseTemplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import static org.lamb.framework.common.enums.LambExceptionEnum.ES00000000;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Component
@RestControllerAdvice
@Order(-1)
@Slf4j
public class LambGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(LambGlobalExceptionHandler.class);

    @Override
    @ExceptionHandler(Exception.class)
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        return write(serverWebExchange.getResponse(),handleTransferException(throwable));
    }

    private LambResponseTemplete handleTransferException(Throwable e) {
        logger.error("GlobalExceptionHandle",e);
        if(e instanceof LambGlobalException){
            return result(((LambGlobalException)e).getCode(),e.getMessage());
        }  else if(e instanceof WebExchangeBindException || e instanceof MethodArgumentNotValidException){
            return result(LambExceptionEnum.EI00000000.getCode(), LambExceptionEnum.EI00000000.getMessage());
        }else if(e instanceof NoSuchElementException){
            return result(LambExceptionEnum.ES00000027.getCode(), LambExceptionEnum.ES00000027.getMessage());
        }
        return result(ES00000000.getCode(),ES00000000.getMessage());
    }

    private LambResponseTemplete result(String code, String message){
        LambResponseTemplete lambResponseTemplete = new LambResponseTemplete();
        lambResponseTemplete.setServiceCode(code);
        lambResponseTemplete.setServiceMessage(message);
        return lambResponseTemplete;
    }



    private  <T> Mono<Void> write(ServerHttpResponse httpResponse, T object) {
        httpResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        httpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return httpResponse
                .writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = httpResponse.bufferFactory();
                    try {
                        return bufferFactory.wrap((new ObjectMapper()).writeValueAsBytes(object));
                    } catch (Exception ex) {
                        logger.warn("Error writing response", ex);
                        return bufferFactory.wrap(new byte[0]);
                    }
                }));
    }




}
