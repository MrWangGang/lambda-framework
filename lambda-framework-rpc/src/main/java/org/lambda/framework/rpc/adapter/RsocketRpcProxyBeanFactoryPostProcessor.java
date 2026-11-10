package org.lambda.framework.rpc.adapter;

import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.RpcRoutes;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.UiFlux;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;

public class RsocketRpcProxyBeanFactoryPostProcessor implements BeanPostProcessor {
    private static final MimeType FORY_MIME_TYPE = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);

    private final Map<ProxyKey, Object> proxies = new ConcurrentHashMap<>();

    private record ProxyKey(Class<?> facade, String service) {}

    private final ObjectProvider<RSocketLoadbalance> loadbalance;
    private final PrincipalStash principalStash;

    public RsocketRpcProxyBeanFactoryPostProcessor(ObjectProvider<RSocketLoadbalance> loadbalance,
                                                   ObjectProvider<PrincipalStash> principalStash) {
        this.loadbalance = loadbalance;
        this.principalStash = () -> Mono.defer(() -> principalStash.getObject().setSecurityStash());
    }

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
                String service = field.getAnnotation(RSocketRpc.class).value();
                field.set(bean, proxies.computeIfAbsent(new ProxyKey(facade, service), this::createProxy));
            } catch (IllegalAccessException error) {
                throw new EventException(ES_RPC_016);
            }
        }
    }

    private Object createProxy(ProxyKey key) {
        Class<?> facade = key.facade();
        var routes = RpcRoutes.routes(key.service(), facade);
        var connection = Mono.defer(() -> loadbalance.getObject().build(FORY_MIME_TYPE, key.service()));
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

}
