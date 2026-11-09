package org.lambda.framework.rsocket.adapter;

import io.rsocket.frame.FrameType;
import org.lambda.framework.common.annotation.rsocket.GatewayStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
class GatewayStreamContractController {
    private final ObjectProvider<RSocketMessageHandler> handlers;

    GatewayStreamContractController(ObjectProvider<RSocketMessageHandler> handlers) {
        this.handlers = handlers;
    }

    @MessageMapping(GatewayStream.DISCOVERY_ROUTE)
    Mono<List<String>> streamRoutes() {
        return Mono.fromSupplier(() -> handlers.getObject().getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().hasMethodAnnotation(GatewayStream.class))
                .filter(entry -> entry.getKey().getCondition(RSocketFrameTypeMessageCondition.class)
                        .getFrameTypes().contains(FrameType.REQUEST_STREAM))
                .flatMap(entry -> entry.getKey().getCondition(DestinationPatternsMessageCondition.class)
                        .getPatterns().stream())
                .filter(path -> path.startsWith("/"))
                .distinct()
                .sorted()
                .toList());
    }
}
