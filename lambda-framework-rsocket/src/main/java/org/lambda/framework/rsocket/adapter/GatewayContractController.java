package org.lambda.framework.rsocket.adapter;

import io.rsocket.frame.FrameType;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.common.support.UiFlux;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.function.SingletonSupplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
class GatewayContractController implements GatewayRouteContract {
    private final ObjectProvider<RSocketMessageHandler> handlers;
    private final SingletonSupplier<Map<String, GatewayInteraction>> routes = SingletonSupplier.of(this::scanRoutes);
    private final SingletonSupplier<String> version = SingletonSupplier.of(() -> GatewayRouteContract.versionOf(routes.obtain()));

    GatewayContractController(ObjectProvider<RSocketMessageHandler> handlers) {
        this.handlers = handlers;
    }

    @MessageMapping(GatewayInteraction.DISCOVERY_ROUTE)
    Mono<Map<String, GatewayInteraction>> interactions() {
        return Mono.fromSupplier(routes::obtain);
    }

    @Override
    public String version() {
        return version.obtain();
    }

    private Map<String, GatewayInteraction> scanRoutes() {
        Map<String, EnumSet<GatewayInteraction>> byPath = new HashMap<>();
        handlers.getObject().getHandlerMethods().forEach((mapping, handler) -> {
            var frames = mapping.getCondition(RSocketFrameTypeMessageCondition.class).getFrameTypes();
            var interaction = classify(ResolvableType.forMethodReturnType(handler.getMethod(), handler.getBeanType()), frames);
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
    }

    private static GatewayInteraction classify(ResolvableType result, Set<FrameType> frames) {
        Class<?> type = result.toClass();
        if (frames.contains(FrameType.REQUEST_CHANNEL) && type == BiFlux.class) {
            return GatewayInteraction.REQUEST_CHANNEL;
        }
        if (Flux.class.isAssignableFrom(type) && frames.contains(FrameType.REQUEST_STREAM)) {
            return type == UiFlux.class || type == BiFlux.class
                    ? GatewayInteraction.REQUEST_STREAM : GatewayInteraction.REQUEST_ARRAY;
        }
        if (Mono.class.isAssignableFrom(type) && frames.contains(FrameType.REQUEST_RESPONSE)) {
            return result.getGeneric(0).resolve() == Void.class
                    ? GatewayInteraction.REQUEST_VOID : GatewayInteraction.REQUEST_RESPONSE;
        }
        return null;
    }
}
