package org.lambda.framework.common.support;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RpcRoutes {
    private static final Pattern SERVICE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*");

    private RpcRoutes() {}

    public static boolean isFacade(Class<?> type) {
        return type.isInterface() && type.getSimpleName().endsWith("Facade");
    }

    public static List<Method> methods(Class<?> facade) {
        return Arrays.stream(facade.getMethods()).filter(method -> Modifier.isAbstract(method.getModifiers())
                && !method.isSynthetic()).toList();
    }

    public static Map<Method, String> routes(String service, Class<?> facade) {
        if (!SERVICE_NAME.matcher(service).matches()) {
            throw new IllegalArgumentException("Expected a Nacos service name for Facade " + facade.getName());
        }
        if (!isFacade(facade)) {
            throw new IllegalArgumentException("Expected an interface named *Facade: " + facade.getName());
        }
        String prefix = service + "." + StringUtils.uncapitalizeAsProperty(facade.getSimpleName()) + ".";
        Map<Method, String> routes = new LinkedHashMap<>();
        Map<String, Method> byRoute = new HashMap<>();
        for (Method method : methods(facade)) {
            String route = prefix + method.getName();
            Method previous = byRoute.putIfAbsent(route, method);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate RSocket route " + route + ": " + previous + " and " + method);
            }
            validateSignature(method);
            routes.put(method, route);
        }
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("Facade declares no RPC methods: " + facade.getName());
        }
        return Map.copyOf(routes);
    }

    private static void validateSignature(Method method) {
        var result = method.getReturnType();
        if (!(method.getGenericReturnType() instanceof ParameterizedType)
                || (result != Mono.class && result != Flux.class
                    && result != BiFlux.class && result != UiFlux.class)) {
            throw new IllegalArgumentException("Facade must return a typed Mono, Flux, UiFlux or BiFlux: " + method);
        }
        if (method.getParameterCount() > 1) {
            throw new IllegalArgumentException("Facade accepts at most one payload: " + method);
        }
        if (method.getParameterCount() == 1) {
            var adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(method.getParameterTypes()[0]);
            if (adapter != null && adapter.isMultiValue() && (result != BiFlux.class
                    || !(method.getGenericParameterTypes()[0] instanceof ParameterizedType))) {
                throw new IllegalArgumentException("A streaming request requires a typed publisher and BiFlux<O> return: " + method);
            }
        }
    }
}
