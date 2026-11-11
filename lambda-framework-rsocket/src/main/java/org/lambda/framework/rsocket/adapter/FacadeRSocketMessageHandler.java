package org.lambda.framework.rsocket.adapter;


import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.RpcRoutes;
import org.lambda.framework.enums.RpcExceptionEnum;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class FacadeRSocketMessageHandler extends RSocketMessageHandler {
    private final String service;
    private final Map<Class<?>, Map<Method, String>> facadeMappings = new HashMap<>();
    private final Map<String, HandlerMethod> registeredRoutes = new HashMap<>();

    FacadeRSocketMessageHandler(String service) {
        this.service = service;
    }

    @Override
    protected CompositeMessageCondition getMappingForMethod(Method method, Class<?> handlerType) {
        Class<?> userType = ClassUtils.getUserClass(handlerType);
        if (!AnnotatedElementUtils.hasAnnotation(userType, RSocketFacade.class)) {
            return super.getMappingForMethod(method, handlerType);
        }
        String route = facadeMappings.computeIfAbsent(userType, this::mapFacade)
                .get(BridgeMethodResolver.findBridgedMethod(method));
        if (super.getMappingForMethod(method, handlerType) != null) {
            throw new EventException(RpcExceptionEnum.INVALID_FACADE, "Use an ordinary Controller for explicit mappings: " + method);
        }
        return route == null ? null : new CompositeMessageCondition(RSocketFrameTypeMessageCondition.EMPTY_CONDITION,
                new DestinationPatternsMessageCondition(new String[]{route}, obtainRouteMatcher()));
    }

    @Override
    protected CompositeMessageCondition extendMapping(CompositeMessageCondition mapping, HandlerMethod method) {
        var destinations = mapping.getCondition(DestinationPatternsMessageCondition.class);
        if (destinations != null) {
            for (String route : destinations.getPatterns()) {
                var previous = registeredRoutes.putIfAbsent(route, method);
                if (previous != null) {
                    throw new EventException(RpcExceptionEnum.INVALID_FACADE, "Duplicate RSocket route " + route + ": "
                            + previous.getBean() + " (" + previous + ") and "
                            + method.getBean() + " (" + method + ")");
                }
            }
        }
        return super.extendMapping(mapping, method);
    }

    private Map<Method, String> mapFacade(Class<?> implementation) {
        var candidates = ClassUtils.getAllInterfacesForClassAsSet(implementation).stream()
                .filter(RpcRoutes::isFacade).toList();
        var facades = candidates.stream()
                .filter(type -> candidates.stream().noneMatch(other -> type != other && type.isAssignableFrom(other)))
                .toList();
        if (facades.size() != 1) {
            throw new EventException(RpcExceptionEnum.INVALID_FACADE, implementation.getName() + " must implement one *Facade interface");
        }
        var routes = new HashMap<Method, String>();
        RpcRoutes.routes(service, facades.getFirst()).forEach((method, route) ->
                routes.put(BridgeMethodResolver.getMostSpecificMethod(method, implementation), route));
        return routes;
    }
}
