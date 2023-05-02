package org.lambda.framework.web.security;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.web.security.container.AuthToken;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


import static org.lambda.framework.common.enums.ExceptionEnum.*;
import static org.lambda.framework.web.security.contract.Contract.*;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
@Component
public class PrincipalUtil {

    @Resource(name = "securityAuthRedisTemplate")
    private ReactiveRedisTemplate securityAuthRedisTemplate;

    public Mono<String> getPrincipalByToken(String key){
        return ReactiveRedisOperation.<String,String>build(securityAuthRedisTemplate).get(key);
    }

    public String setPrincipalToToken(String principal){
        if(StringUtils.isBlank(principal)){
            throw new EventException(EA00000002);
        }
        String key = LAMBDA_SECURITY_AUTH_TOKEN_KEY+ MD5Util.hash(principal+"."+LAMBDA_SECURITY_AUTH_TOKEN_SALT);
        ReactiveRedisOperation.build(securityAuthRedisTemplate).set(key,principal,LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue());
        return key;
    }

    public static String getCredentials(){
        AuthToken authentication = getAuthentication();
        String credentials = authentication.getCredentials();
        if(StringUtils.isBlank(credentials))throw new EventException(EA00000003);
        return credentials;
    }

    public static <T>T getPrincipal(Class<T> clazz) {
        if(clazz == null)throw new EventException(EA00000006);
        AuthToken authentication = getAuthentication();
        String principal = authentication.getPrincipal();
        if(StringUtils.isBlank(principal))throw new EventException(EA00000004);
        try{
            return (T)(JsonUtil.stringToObj(principal,clazz).orElseThrow(()->new EventException(EA00000009)));
        }catch (EventException e){
            if(e == null){
                throw new EventException(EA00000009);
            }
            throw new GlobalException(e.getCode(),e.getMessage());
        }catch (ClassCastException e){
            throw new EventException(EA00000009);
        } catch (Throwable throwable) {
            throw new EventException(EA00000009);
        }

    }

    public static AuthToken getAuthentication(){
        if(SecurityContextHolder.getContext() == null)throw new EventException(EA00000008);
        if(SecurityContextHolder.getContext().getAuthentication() == null)throw new EventException(EA00000000);
        try {
            AuthToken authentication = (AuthToken) SecurityContextHolder.getContext().getAuthentication();
            return authentication;
        }catch (Exception e){
            throw new EventException(EA00000009);
        }
    }
}
