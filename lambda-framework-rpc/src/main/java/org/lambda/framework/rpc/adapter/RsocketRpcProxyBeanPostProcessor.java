package org.lambda.framework.rpc.adapter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.lambda.framework.rpc.annotation.rsocket.RSocketRpcMapping;
import org.lambda.framework.rpc.annotation.rsocket.RsocketRpc;
import org.lambda.framework.rpc.annotation.rsocket.RsocketRpcDiscorvery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_DIRECT;
import static org.lambda.framework.rpc.enums.RpcConstant.RPC_CONNECT_LOADBALANCE;
import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;

@Component
public class RsocketRpcProxyBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    @Lazy
    @Resource
    private RSocketLoadbalance rSocketLoadbalance;

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();
        if(this.verifys(fields)){
            for (Field field : fields) {
                if (field.isAnnotationPresent(RsocketRpc.class)) {
                    //@RsocketRpc 只能标记在属性上，且必须是接口类型
                    if(field.getType().isInterface()){
                        //获取这个类
                        Class<?> interfaceField = getClasses(field.getType());
                        if(interfaceField.isAnnotationPresent(RsocketRpcDiscorvery.class)){
                            RsocketRpcDiscorvery rsocketRpcDiscorvery = AnnotationUtils.findAnnotation(interfaceField, RsocketRpcDiscorvery.class);
                            //确认是否为直连模式还是负载均衡模式
                            if(rsocketRpcDiscorvery == null){
                                throw new EventException(ES_RPC_002);
                            }
                            Assert.verify(rsocketRpcDiscorvery.value(),ES_RPC_004);
                            String rsocketUrl = rsocketRpcDiscorvery.value();
                            String connectType =  confirmConnectType(rsocketUrl);
                            return createProxy(rsocketUrl,connectType,interfaceField);
                        }
                        throw new EventException(ES_RPC_002);
                    }
                    throw new EventException(ES_RPC_000);
                }
            }
        }
        return bean;
    }

    private Object createProxy(String rsocketUrl,String connectType,Class<?> interfaceField) {
        Assert.verify(interfaceField,ES_RPC_009);
        Assert.verify(rsocketUrl,ES_RPC_008);
        Assert.verify(connectType,ES_RPC_007);
        return Proxy.newProxyInstance(
                interfaceField.getClassLoader(),
                new Class[]{interfaceField},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RSocketRpcMapping rSocketRpcMapping = AnnotationUtils.findAnnotation(method, RSocketRpcMapping.class);
                        if (rSocketRpcMapping != null) {
                            if(Assert.verify(rSocketRpcMapping.value())){
                                //校验头
                                verifyType(rSocketRpcMapping.MimeType());
                                String route = rSocketRpcMapping.value();
                                if(RPC_CONNECT_LOADBALANCE.equals(connectType)){
                                    //负载均衡模式
                                    Mono<RSocketRequester> rSocketRequesterMono =  rSocketLoadbalance.build(rsocketUrl,MediaType.valueOf(rSocketRpcMapping.MimeType()));
                                    return getResult(route,getData(args),method,rSocketRequesterMono);
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
                                        Mono<RSocketRequester> rSocketRequesterMono = rSocketLoadbalance.build(parts[0],port,MediaType.valueOf(rSocketRpcMapping.MimeType()));
                                        return getResult(route,getData(args),method,rSocketRequesterMono);
                                    }
                                    throw new EventException(ES_RPC_012);
                                }
                            }
                            throw new EventException(ES_RPC_006);
                        }
                        throw new EventException(ES_RPC_003);
                    }

                });
    }

    private Object getResult(String route, Object data,Method method,Mono<RSocketRequester> rSocketRequesterMono){
        return rSocketRequesterMono.switchIfEmpty(Mono.error(new EventException(ES_RPC_013))).flatMap(req->{
            RSocketRequester.RetrieveSpec retrieveSpec = req.route(route).data(data);
            Type returnType = method.getGenericReturnType();
            if(returnType instanceof ParameterizedType parameterizedType){
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if(verifys(actualTypeArguments)){
                    if(actualTypeArguments.length!=1){
                        return Mono.error(new EventException(ES_RPC_015));
                    }
                }
                if (parameterizedType.getRawType() == Mono.class) {
                    if(actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class){
                        return retrieveSpec.retrieveMono(String.class);
                    }
                    return retrieveSpec.retrieveMono(Object.class);
                }
                if (parameterizedType.getRawType() == Flux.class) {
                    if(actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class){
                        return retrieveSpec.retrieveMono(String.class);
                    }
                    return retrieveSpec.retrieveMono(Object.class);
                }
            }
            return Mono.error(new EventException(ES_RPC_015));
        });
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
        // 使用正则表达式匹配纯字母组成
        String regex = "[a-zA-Z]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
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

    private static Class<?> getClasses(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        if(interfaces == null){
            throw new EventException(ES_RPC_000);
        }
        if(interfaces.length == 0){
            throw new EventException(ES_RPC_000);
        }
        if(interfaces[0] == null){
            throw new EventException(ES_RPC_000);
        }
        if(interfaces.length != 1){
            throw new EventException(ES_RPC_001);
        }
        return interfaces[0];
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
