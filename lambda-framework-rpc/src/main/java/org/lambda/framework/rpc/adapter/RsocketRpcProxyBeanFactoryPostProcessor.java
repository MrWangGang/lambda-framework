package org.lambda.framework.rpc.adapter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.UiFlux;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;

@Component
public class RsocketRpcProxyBeanFactoryPostProcessor implements BeanPostProcessor {
    private static final MimeType FORY_MIME_TYPE = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);

    private final Map<Class<?>, Object> proxies = new ConcurrentHashMap<>();

    @Resource
    private RSocketLoadbalance rSocketLoadbalance;

    @Resource
    private PrincipalStash principalStash;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 处理带有 @RsocketRpc 注解的字段
        Class<?> beanClass = bean.getClass();
        this.inspectClassProperties(beanClass,bean);
        return bean;
    }

    public void inspectClassProperties(Class<?> clazz,Object bean) throws BeansException {
        for (Field field : getAllFields(clazz)) {
            Class<?> facade = field.getType();
            if (!facade.isInterface()) {
                throw new EventException(ES_RPC_000);
            }
            try {
                field.set(bean, proxies.computeIfAbsent(facade, this::createProxy));
            } catch (IllegalAccessException error) {
                throw new EventException(ES_RPC_016);
            }
        }
    }

    private Object createProxy(Class<?> facade) {
        String service = null;
        Map<Method, String> routes = new HashMap<>();
        for (Method method : facade.getMethods()) {
            var mapping = AnnotationUtils.findAnnotation(method, MessageMapping.class);
            if (Modifier.isStatic(method.getModifiers()) || (method.isDefault() && mapping == null)) {
                continue;
            }
            if (mapping == null) {
                throw new IllegalArgumentException("Declare @MessageMapping on " + method);
            }
            validateSignature(method);
            var nativeRoutes = Arrays.stream(mapping.value()).filter(path -> !path.startsWith("/")).toList();
            if (nativeRoutes.size() != 1) {
                throw new IllegalArgumentException("Expected one native @MessageMapping route on " + method);
            }
            String route = nativeRoutes.getFirst();
            int separator = route.indexOf('.');
            if (separator <= 0 || separator == route.length() - 1) {
                throw new IllegalArgumentException("Expected a service prefix and method route on " + method);
            }
            String target = route.substring(0, separator);
            if (target.isBlank() || (service != null && !service.equals(target))) {
                throw new IllegalArgumentException("Facade routes must share one service prefix: " + facade.getName());
            }
            service = target;
            routes.put(method, route);
        }
        Assert.verify(service, ES_RPC_004);
        var connection = rSocketLoadbalance.build(FORY_MIME_TYPE, service);
        return Proxy.newProxyInstance(facade.getClassLoader(), new Class<?>[]{facade}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "RSocket facade " + facade.getName();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new EventException(ES_RPC_003);
                };
            }
            String route = routes.get(method);
            if (route != null) {
                return getResult(route, getData(args), method, connection);
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            throw new EventException(ES_RPC_003);
        });
    }

    private Object getResult(String route, Object data, Method method,
                             Mono<RSocketLoadbalance.ProcessResponse> requester) {
        var returnType = (ParameterizedType) method.getGenericReturnType();
        var resultType = returnType.getRawType();
        if (resultType != BiFlux.class && data instanceof Publisher<?> && !(data instanceof Mono<?>)) {
            throw new IllegalArgumentException("A streaming request requires BiFlux<O> return: " + method);
        }
        var bodyType = ParameterizedTypeReference.forType(returnType.getActualTypeArguments()[0]);
        var connection = requester.switchIfEmpty(Mono.error(new EventException(ES_RPC_013)));
        if (resultType == Mono.class) {
            return connection.flatMap(rpc -> rpc.retrieveMono(principalStash, route, data, bodyType));
        }
        var values = connection.flatMapMany(rpc -> rpc.retrieveFlux(principalStash, route, data, bodyType));
        if (resultType == Flux.class) {
            return values;
        }
        if (resultType == UiFlux.class) {
            return UiFlux.of(values);
        }
        return BiFlux.of(values);
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();
        getAllFieldsRecursive(clazz, allFields);
        return allFields;
    }

    private static void getAllFieldsRecursive(Class<?> clazz, List<Field> fields) {
        if (clazz != null && clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                if(field.isAnnotationPresent(RSocketRpc.class)){
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            getAllFieldsRecursive(clazz.getSuperclass(), fields);
        }
    }

    private Object getData(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return new byte[0];
        }
        if (arguments.length != 1 || arguments[0] == null) {
            throw new EventException(ES_RPC_014);
        }
        return arguments[0];
    }

    private static void validateSignature(Method method) {
        var adapters = ReactiveAdapterRegistry.getSharedInstance();
        var parameters = method.getParameterTypes();
        boolean streamingInput = Arrays.stream(parameters).anyMatch(type -> {
            var adapter = adapters.getAdapter(type);
            return adapter != null && adapter.isMultiValue();
        });
        var result = method.getReturnType();
        if (!(method.getGenericReturnType() instanceof ParameterizedType)
                || (result != Mono.class && result != Flux.class && result != BiFlux.class && result != UiFlux.class)) {
            throw new EventException(ES_RPC_015);
        }
        if (parameters.length > 1) {
            throw new EventException(ES_RPC_014);
        }
        if (streamingInput && (result != BiFlux.class
                || !(method.getGenericParameterTypes()[0] instanceof ParameterizedType))) {
            throw new IllegalArgumentException("A streaming request requires a typed publisher and BiFlux<O> return: " + method);
        }
    }
}
