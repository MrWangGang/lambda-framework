package org.lambda.framework.rpc.adapter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.TFlux;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lambda.framework.rpc.adapter.support.CamelCase.buildMessageMapping;
import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_DIRECT;
import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_LOADBALANCE;
import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;

@Component
public class RsocketRpcProxyBeanFactoryPostProcessor implements BeanPostProcessor {
    private static final MimeType FORY_MIME_TYPE = MimeType.valueOf(RSocketRpcType.FORY_MIME_TYPE);

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
        var discovery = AnnotationUtils.findAnnotation(facade, RSocketRpcDiscorvery.class);
        String service = discovery == null ? null : discovery.value();
        if (discovery != null) {
            Assert.verify(service, ES_RPC_004);
        }
        Map<Method, String> routes = new HashMap<>();
        for (Method method : facade.getMethods()) {
            var mapping = AnnotationUtils.findAnnotation(method, MessageMapping.class);
            if (Modifier.isStatic(method.getModifiers())
                    || (method.isDefault() && mapping == null && !method.isAnnotationPresent(RSocketRpcType.class))) {
                continue;
            }
            validateChannelSignature(method);
            String route;
            if (mapping != null) {
                var nativeRoutes = Arrays.stream(mapping.value()).filter(path -> !path.startsWith("/")).toList();
                if (nativeRoutes.size() != 1 || !nativeRoutes.getFirst().contains(".")) {
                    throw new IllegalArgumentException("Expected one native @MessageMapping route on " + method);
                }
                route = nativeRoutes.getFirst();
                if (discovery == null) {
                    String target = route.substring(0, route.indexOf('.'));
                    if (target.isBlank() || (service != null && !service.equals(target))) {
                        throw new IllegalArgumentException("Facade routes must share one service prefix: " + facade.getName());
                    }
                    service = target;
                }
            } else {
                if (discovery == null) {
                    throw new IllegalArgumentException("Declare @MessageMapping on " + method);
                }
                route = buildMessageMapping(service, facade, method);
            }
            routes.put(method, route);
        }
        Assert.verify(service, ES_RPC_004);
        var connection = requester(service);
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

    private Mono<RSocketLoadbalance.ProcessResponse> requester(String target) {
        String connectType = confirmConnectType(target);
        if (RPC_CONNECT_LOADBALANCE.equals(connectType)) {
            return rSocketLoadbalance.build(FORY_MIME_TYPE, target);
        }
        String[] parts = target.split(":");
        try {
            return rSocketLoadbalance.build(FORY_MIME_TYPE, parts[0], Integer.valueOf(parts[1]));
        } catch (NumberFormatException error) {
            throw new EventException(ES_RPC_012);
        }
    }

    private Object getResult(String route, Object data, Method method,
                             Mono<RSocketLoadbalance.ProcessResponse> requester) {
        if (!(method.getGenericReturnType() instanceof ParameterizedType returnType)
                || returnType.getActualTypeArguments().length != 1) {
            return Mono.error(new EventException(ES_RPC_015));
        }
        var resultType = returnType.getRawType();
        if ((resultType == TFlux.class && !(data instanceof TFlux<?>))
                || (resultType != BiFlux.class && resultType != TFlux.class
                    && data instanceof Publisher<?> && !(data instanceof Mono<?>))) {
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
        if (resultType == BiFlux.class) {
            return BiFlux.of(values);
        }
        if (resultType == TFlux.class) {
            return TFlux.of(values);
        }
        return Mono.error(new EventException(ES_RPC_015));
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

    private Object getData(Object...datas){
        if(verifys(datas)){
            if(datas.length!=1){
                throw new EventException(ES_RPC_014);
            }
            return datas[0];
        }
        return new byte[0];
    }



    private static void validateChannelSignature(Method method) {
        var adapters = ReactiveAdapterRegistry.getSharedInstance();
        var parameters = method.getParameterTypes();
        boolean streamingInput = Arrays.stream(parameters).anyMatch(type -> {
            var adapter = adapters.getAdapter(type);
            return adapter != null && adapter.isMultiValue();
        });
        var result = method.getReturnType();
        if (result == TFlux.class) {
            if (parameters.length != 1 || parameters[0] != TFlux.class
                    || !(method.getGenericParameterTypes()[0] instanceof ParameterizedType)
                    || !(method.getGenericReturnType() instanceof ParameterizedType)) {
                throw new IllegalArgumentException("Legacy TFlux requires TFlux<I> input and TFlux<O> return: " + method);
            }
        } else if (result == BiFlux.class || result == UiFlux.class || streamingInput) {
            if (!(method.getGenericReturnType() instanceof ParameterizedType)
                    || (streamingInput && (result != BiFlux.class || parameters.length != 1
                        || !(method.getGenericParameterTypes()[0] instanceof ParameterizedType)))) {
                throw new IllegalArgumentException("A streaming request requires a typed publisher and BiFlux<O> return: " + method);
            }
        }
    }




    private String confirmConnectType(String value){
        Assert.verify(value,ES_RPC_004);
        if(isString(value)){
            return RPC_CONNECT_LOADBALANCE;
        }

        if(isIpPort(value)){
            return RPC_CONNECT_DIRECT;
        }
        throw new EventException(ES_RPC_005);
    }


    public static boolean isString(String input) {
        //支持任何的名字
        return true;
    }

    public static boolean isIpPort(String input) {
        // 使用正则表达式匹配 xxx.xxx.xxx.xxx:port 形式
        String regex = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}:(?:[1-9]\\d{0,4}|[1-5]\\d{4}|6[0-5]{4}|6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-9]\\d{0,3})\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            return false; // 不匹配正则表达式，不合法
        }

        // 拆分 IP 地址和端口号
        String[] parts = input.split(":");
        String ipAddress = parts[0];
        String port = parts[1];

        // 检查 IP 地址每个段的范围是否合规
        String[] ipSegments = ipAddress.split("\\.");
        for (String segment : ipSegments) {
            int segmentValue = Integer.parseInt(segment);
            if (segmentValue < 0 || segmentValue > 255) {
                return false; // 段不在合法范围内，不合法
            }
        }

        // 检查端口号的范围是否合规
        int portValue = Integer.parseInt(port);
        if (portValue < 1 || portValue > 65535) {
            return false; // 端口号不在合法范围内，不合法
        }

        return true; // IP 地址和端口号合法
    }

    private boolean verifys(Object...objects){
        if(objects!=null){
            if(objects.length!=0){
                if(objects[0]!=null){
                    return true;
                }
            }
        }
        return false;
    }

}
