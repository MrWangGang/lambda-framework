package org.lambda.framework.rsocket.adapter;

import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.lambda.framework.common.support.RpcRoutes;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class ForyRpcContracts {
    private final Set<ResolvableType> visited = new HashSet<>();
    private final Set<Class<?>> types = new LinkedHashSet<>();

    static Set<Class<?>> discover(ListableBeanFactory beans) {
        var contracts = new ForyRpcContracts();
        for (String name : beans.getBeanNamesForType(Object.class, true, false)) {
            Class<?> beanType = beans.getType(name, false);
            if (beanType != null) {
                ReflectionUtils.doWithFields(beanType, field -> {
                    Class<?> facade = field.getType();
                    if (facade.isInterface()) {
                        RpcRoutes.methods(facade).forEach(method -> contracts.addMethod(method, facade));
                    }
                }, field -> field.isAnnotationPresent(RSocketRpc.class));
            }
        }
        beans.getBeanProvider(RSocketMessageHandler.class).ifAvailable(handler ->
                handler.getHandlerMethods().forEach((mapping, method) -> {
                    var destinations = mapping.getCondition(DestinationPatternsMessageCondition.class);
                    if (AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RSocketFacade.class)
                            && destinations != null && destinations.getPatterns().stream()
                                    .anyMatch(path -> !path.startsWith("/"))) {
                        contracts.addMethod(BridgeMethodResolver.findBridgedMethod(method.getMethod()), method.getBeanType());
                    }
                }));
        return contracts.types;
    }

    private void addMethod(Method method, Class<?> owner) {
        var activeRecords = new HashSet<Class<?>>();
        add(ResolvableType.forMethodReturnType(method, owner), activeRecords);
        for (int index = 0; index < method.getParameterCount(); index++) {
            add(ResolvableType.forMethodParameter(method, index, owner), activeRecords);
        }
    }

    private void add(ResolvableType type, Set<Class<?>> activeRecords) {
        Class<?> raw = type.resolve();
        if (raw == null || !visited.add(type)) {
            return;
        }
        Arrays.stream(type.getGenerics()).forEach(generic -> add(generic, activeRecords));
        if (raw.isArray()) {
            add(type.getComponentType(), activeRecords);
        } else if (raw.isRecord() || raw.isEnum()
                || raw.getName().equals("org.lambda.framework.repository.operation.Paged")) {
            types.add(raw);
        }
        if (raw.isRecord() && activeRecords.add(raw)) {
            for (var component : raw.getRecordComponents()) {
                add(ResolvableType.forType(component.getGenericType(), type), activeRecords);
            }
            activeRecords.remove(raw);
        }
    }
}
