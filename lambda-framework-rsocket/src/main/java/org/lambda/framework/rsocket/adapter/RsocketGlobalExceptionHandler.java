package org.lambda.framework.rsocket.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import reactor.core.publisher.Mono;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Component
@Order(-2)
@Slf4j
@ControllerAdvice
public class RsocketGlobalExceptionHandler{
    @MessageExceptionHandler(Exception.class)
    public Mono<Throwable> handleException(Throwable ex) {
        return Mono.error(ex);
    }
}
