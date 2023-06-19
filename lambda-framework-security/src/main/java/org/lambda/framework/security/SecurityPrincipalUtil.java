package org.lambda.framework.security;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.container.SecurityAuthToken;
import org.lambda.framework.security.contract.SecurityContract;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
@Component
public class SecurityPrincipalUtil {

    @Resource(name = "securityAuthRedisOperation")
    private ReactiveRedisOperation securityAuthRedisOperation;

    public Mono<String> getPrincipalByToken(String key){
        return securityAuthRedisOperation.get(key);
    }

    public Mono<String> setPrincipalToToken(String principal){
        if(StringUtils.isBlank(principal)){
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_002);
        }
        String key = SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY+ MD5Util.hash(principal+"."+ SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_SALT);
        return securityAuthRedisOperation.set(key,principal, SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue()).then(Mono.just(key));
    }

    public Mono<Void> deletePrincipalByToken(String authToken){
        if(StringUtils.isBlank(authToken)){
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_003);
        }
        return getPrincipalByToken(authToken).switchIfEmpty(Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004))).map(e->{
            return e;
        }).map(e->{
             return securityAuthRedisOperation.delete(e);
        }).then(Mono.empty());
    }


    //这些方法不是响应式的可能会出问题，不要使用
    @Deprecated
    public static String getCredentials(){
        SecurityAuthToken authentication = getAuthentication();
        String credentials = authentication.getCredentials();
        if(StringUtils.isBlank(credentials))throw new EventException(SecurityExceptionEnum.ES_SECURITY_003);
        return credentials;
    }

    @Deprecated
    public static <T>T getPrincipal(Class<T> clazz) {
        if(clazz == null)throw new EventException(SecurityExceptionEnum.ES_SECURITY_006);
        SecurityAuthToken authentication = getAuthentication();
        String principal = authentication.getPrincipal();
        if(StringUtils.isBlank(principal))throw new EventException(SecurityExceptionEnum.ES_SECURITY_004);
        try{
            return (T)(JsonUtil.stringToObj(principal,clazz).orElseThrow(()->new EventException(SecurityExceptionEnum.ES_SECURITY_009)));
        }catch (EventException e){
            if(e == null){
                throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
            }
            throw new GlobalException(e.getCode(),e.getMessage());
        }catch (ClassCastException e){
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        } catch (Throwable throwable) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        }

    }

    @Deprecated
    public static SecurityAuthToken getAuthentication(){
        if(SecurityContextHolder.getContext() == null)throw new EventException(SecurityExceptionEnum.ES_SECURITY_008);
        if(SecurityContextHolder.getContext().getAuthentication() == null)throw new EventException(SecurityExceptionEnum.ES_SECURITY_000);
        try {
            SecurityAuthToken authentication = (SecurityAuthToken) SecurityContextHolder.getContext().getAuthentication();
            return authentication;
        }catch (Exception e){
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        }
    }
}
