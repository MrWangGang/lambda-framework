package org.lamb.framework.web.security;

import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.common.exception.basic.LambGlobalException;
import org.lamb.framework.common.util.sample.JsonUtil;
import org.lamb.framework.common.util.sample.MD5Util;
import org.lamb.framework.redis.sub.operation.LambReactiveRedisOperation;
import org.lamb.framework.web.security.container.LambAuthToken;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.web.security.contract.Contract.*;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
@Component
public class LambPrincipalUtil {

    @Resource(name = "lambAuthRedisTemplate")
    private ReactiveRedisTemplate lambAuthRedisTemplate;

    public Mono<String> getPrincipalByToken(String key){
        return LambReactiveRedisOperation.<String,String>build(lambAuthRedisTemplate).get(key);
    }

    public String setPrincipalToToken(String principal){
        if(StringUtils.isBlank(principal)){
            throw new LambEventException(EA00000002);
        }
        String key = LAMB_TOKEN_KEY+ MD5Util.hash(principal+"."+LAMB_AUTH_TOKEN_SALT);
        LambReactiveRedisOperation.build(lambAuthRedisTemplate).set(key,principal,LAMB_TOKEN_TIME_SECOND.longValue());
        return key;
    }

    public static String getCredentials(){
        LambAuthToken authentication = getAuthentication();
        String credentials = authentication.getCredentials();
        if(StringUtils.isBlank(credentials))throw new LambEventException(EA00000003);
        return credentials;
    }

    public static <T>T getPrincipal(Class<T> clazz) {
        if(clazz == null)throw new LambEventException(EA00000006);
        LambAuthToken authentication = getAuthentication();
        String principal = authentication.getPrincipal();
        if(StringUtils.isBlank(principal))throw new LambEventException(EA00000004);
        try{
            return (T)(JsonUtil.stringToObj(principal,clazz).orElseThrow(()->new LambEventException(EA00000009)));
        }catch (LambEventException e){
            if(e == null){
                throw new LambEventException(EA00000009);
            }
            throw new LambGlobalException(e.getCode(),e.getMessage());
        }catch (ClassCastException e){
            throw new LambEventException(EA00000009);
        } catch (Throwable throwable) {
            throw new LambEventException(EA00000009);
        }

    }

    public static LambAuthToken getAuthentication(){
        if(SecurityContextHolder.getContext() == null)throw new LambEventException(EA00000008);
        if(SecurityContextHolder.getContext().getAuthentication() == null)throw new LambEventException(EA00000000);
        try {
            LambAuthToken authentication = (LambAuthToken) SecurityContextHolder.getContext().getAuthentication();
            return authentication;
        }catch (Exception e){
            throw new LambEventException(EA00000009);
        }
    }
}
