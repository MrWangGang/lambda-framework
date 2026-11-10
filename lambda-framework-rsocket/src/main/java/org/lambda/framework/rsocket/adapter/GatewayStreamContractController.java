package org.lambda.framework.rsocket.adapter;

import io.rsocket.frame.FrameType;
import org.lambda.framework.common.annotation.rsocket.GatewayStream;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.TFlux;
import org.lambda.framework.common.support.UiFlux;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .filter(GatewayInteraction::isPublicPath)
                .distinct()
                .sorted()
                .toList());
    }

    @MessageMapping(GatewayInteraction.DISCOVERY_ROUTE)
    Mono<Map<String, GatewayInteraction>> interactions() {
        return Mono.fromSupplier(() -> {
            Map<String, EnumSet<GatewayInteraction>> byPath = new HashMap<>();
            handlers.getObject().getHandlerMethods().forEach((mapping, handler) -> {
                var frames = mapping.getCondition(RSocketFrameTypeMessageCondition.class).getFrameTypes();
                var interaction = classify(handler.getMethod().getReturnType(), frames);
                if (interaction != null) {
                    mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns().stream()
                            .filter(GatewayInteraction::isPublicPath)
                            .forEach(path -> byPath.computeIfAbsent(path, ignored -> EnumSet.noneOf(GatewayInteraction.class))
                                    .add(interaction));
                }
            });
            return byPath.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().iterator().next()));
        });
    }

    private static GatewayInteraction classify(Class<?> type, Set<FrameType> frames) {
        if (frames.contains(FrameType.REQUEST_CHANNEL) && (type == BiFlux.class || type == TFlux.class)) {
            return GatewayInteraction.REQUEST_CHANNEL;
        }
        if (Flux.class.isAssignableFrom(type) && frames.contains(FrameType.REQUEST_STREAM)) {
            return type == UiFlux.class || type == BiFlux.class
                    ? GatewayInteraction.REQUEST_STREAM : GatewayInteraction.REQUEST_ARRAY;
        }
        if (Mono.class.isAssignableFrom(type) && frames.contains(FrameType.REQUEST_RESPONSE)) {
            return GatewayInteraction.REQUEST_RESPONSE;
        }
        return null;
    }
}
