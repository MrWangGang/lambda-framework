package org.lambda.framework.rpc.adapter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcType;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lambda.framework.rpc.adapter.support.CamelCase.buildMessageMapping;
import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_DIRECT;
import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_LOADBALANCE;
import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;

@Component
public class RsocketRpcProxyBeanFactoryPostProcessor implements BeanPostProcessor {

    private ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();

    @Resource
    private RSocketLoadbalance rSocketLoadbalance;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 处理带有 @RsocketRpc 注解的字段
        Class<?> beanClass = bean.getClass();
        this.inspectClassProperties(beanClass,bean);
        return bean;
    }

    public void inspectClassProperties(Class<?> clazz,Object bean) throws BeansException {
        if(clazz!=null){
            List<Field> fields = getAllFields(clazz);
            if(this.verifys(fields)){
                for (Field field : fields) {
                    if (field.isAnnotationPresent(RSocketRpc.class)) {
                        //@RsocketRpc 只能标记在属性上，且必须是接口类型
                        if(field.getType().isInterface()){
                            //获取这个类
                            Class<?> interfaceField = field.getType();
                            if(interfaceField.isAnnotationPresent(RSocketRpcDiscorvery.class)){
                                RSocketRpcDiscorvery rsocketRpcDiscorvery = AnnotationUtils.findAnnotation(interfaceField, RSocketRpcDiscorvery.class);
                                if(rsocketRpcDiscorvery == null){
                                    throw new EventException(ES_RPC_002);
                                }
                                Assert.verify(rsocketRpcDiscorvery.value(),ES_RPC_004);
                                String rsocketUrl = rsocketRpcDiscorvery.value();
                                String connectType =  confirmConnectType(rsocketUrl);
                                field.setAccessible(true);
                                try {
                                    //让代理类成为单例
                                    Object _proxy = concurrentHashMap.get(interfaceField);
                                    if(_proxy == null){
                                        Object proxy = createProxy(rsocketUrl,connectType,interfaceField);
                                        field.set(bean, proxy);
                                        concurrentHashMap.put(interfaceField,proxy);
                                    }else {
                                        field.set(bean, _proxy);
                                    }
                                } catch (IllegalAccessException e) {
                                    throw new EventException(ES_RPC_016);
                                }
                                continue;
                            }
                            throw new EventException(ES_RPC_002);
                        }
                        throw new EventException(ES_RPC_000);
                    }
                }
            }
        }
    }


    private Object createProxy(String rsocketUrl, String connectType, Class<?> interfaceField) {
        Assert.verify(interfaceField,ES_RPC_009);
        Assert.verify(rsocketUrl,ES_RPC_008);
        Assert.verify(connectType,ES_RPC_007);
        return Proxy.newProxyInstance(
                interfaceField.getClassLoader(),
                new Class[]{interfaceField},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RSocketRpcType rSocketRpcType = AnnotationUtils.findAnnotation(method, RSocketRpcType.class);
                        if (rSocketRpcType != null) {
                          //校验头
                          verifyType(rSocketRpcType.MimeType());
                          if(RPC_CONNECT_LOADBALANCE.equals(connectType)){
                              //负载均衡模式
                              Mono<RSocketRequester> rSocketRequesterMono =  rSocketLoadbalance.build(rsocketUrl,MediaType.valueOf(rSocketRpcType.MimeType()));
                              return getResult(rsocketUrl,getData(args),method,rSocketRequesterMono, rSocketRpcType.MimeType(),interfaceField);
                          }
                          if(RPC_CONNECT_DIRECT.equals(connectType)){
                              //直连模式
                              //直连模式需要把地址解析出来
                              //因为invoke之前已经校验了rsocketUrl 必须是ip类型，不要在这里做校验，减少调用时候的代码复杂度
                              String[] parts = rsocketUrl.split(":");
                              if(verifys(parts)){
                                  Assert.verify(parts[0],ES_RPC_012);
                                  Assert.verify(parts[1],ES_RPC_012);
                                  Integer port = null;
                                  try {
                                      port = Integer.valueOf(parts[1]);
                                  }catch (Exception e){
                                      throw new EventException(ES_RPC_012);
                                  }
                                  Mono<RSocketRequester> rSocketRequesterMono = rSocketLoadbalance.build(parts[0],port,MediaType.valueOf(rSocketRpcType.MimeType()));
                                  return getResult(rsocketUrl,getData(args),method,rSocketRequesterMono, rSocketRpcType.MimeType(),interfaceField);

                              }
                              throw new EventException(ES_RPC_012);
                          }
                        }
                        throw new EventException(ES_RPC_003);
                    }

                });
    }

    private Object getResult(String serviceName, Object data,Method method,Mono<RSocketRequester> rSocketRequesterMono,String mimeType,Class<?> interfaceField){
        Type returnType = method.getGenericReturnType();
        if(returnType instanceof ParameterizedType parameterizedType){
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if(verifys(actualTypeArguments)){
                if(actualTypeArguments.length!=1){
                    return Mono.error(new EventException(ES_RPC_015));
                }
            }
            String route = buildMessageMapping(serviceName,interfaceField,method);
            if(MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(MediaType.valueOf(mimeType))){
                if (parameterizedType.getRawType() == Mono.class) {
                    if(actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class){
                        return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMap(req->{
                            RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
                            return retrieveSpec.retrieveMono(String.class);
                        });
                    }
                    return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMap(req->{
                        RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
                        Class<?> useClass = null;
                        if(actualTypeArguments[0] instanceof Class<?>){
                            useClass = (Class<?>) actualTypeArguments[0];
                        }
                        if (actualTypeArguments[0] instanceof ParameterizedType) {
                            useClass = (Class<?>)((ParameterizedType) actualTypeArguments[0]).getRawType();
                        }
                        if(useClass == null)throw new EventException(ES_RPC_027);

                        return retrieveSpec.retrieveMono(useClass);
                    });
                }
                if (parameterizedType.getRawType() == Flux.class) {
                    if(actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class){
                        return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMapMany(req->{
                            RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
                            return retrieveSpec.retrieveFlux(String.class);
                        });
                    }
                    return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMapMany(req->{
                        RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
                        Class<?> useClass = null;
                        if(actualTypeArguments[0] instanceof Class<?>){
                             useClass = (Class<?>) actualTypeArguments[0];
                        }
                        if (actualTypeArguments[0] instanceof ParameterizedType) {
                             useClass = (Class<?>)((ParameterizedType) actualTypeArguments[0]).getRawType();
                        }
                        if(useClass == null)throw new EventException(ES_RPC_027);
                        return retrieveSpec.retrieveFlux(useClass);
                    });
                }
            }
            if(MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(MediaType.valueOf(mimeType))){
                return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMapMany(req->{
                    RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
                    Class<?> useClass = null;
                    if(actualTypeArguments[0] instanceof Class<?>){
                        useClass = (Class<?>) actualTypeArguments[0];
                    }
                    if (actualTypeArguments[0] instanceof ParameterizedType) {
                        useClass = (Class<?>)((ParameterizedType) actualTypeArguments[0]).getRawType();
                    }
                    if(useClass == null)throw new EventException(ES_RPC_027);

                    return retrieveSpec.retrieveFlux(useClass);
                });
            }
            throw new EventException(ES_RPC_010);
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



    private boolean verifyType(String type){
        Assert.verify(type,ES_RPC_011);
        if(MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(MediaType.valueOf(type)) || MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(MediaType.valueOf(type)) ){
            return true;
        }
        throw new EventException(ES_RPC_010);
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
