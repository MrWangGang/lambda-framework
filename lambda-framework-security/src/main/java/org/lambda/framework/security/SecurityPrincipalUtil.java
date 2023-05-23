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
import org.lambda.framework.security.enums.SectExceptionEnum;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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

    @Resource(name = "securityAuthRedisTemplate")
    private ReactiveRedisTemplate securityAuthRedisTemplate;

    public Mono<String> getPrincipalByToken(String key){
        return ReactiveRedisOperation.<String,String>build(securityAuthRedisTemplate).get(key);
    }

    public String setPrincipalToToken(String principal){
        if(StringUtils.isBlank(principal)){
            throw new EventException(SectExceptionEnum.ES_SECURITY_002);
        }
        String key = SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY+ MD5Util.hash(principal+"."+ SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_SALT);
        ReactiveRedisOperation.build(securityAuthRedisTemplate).set(key,principal, SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue());
        return key;
    }

    public static String getCredentials(){
        SecurityAuthToken authentication = getAuthentication();
        String credentials = authentication.getCredentials();
        if(StringUtils.isBlank(credentials))throw new EventException(SectExceptionEnum.ES_SECURITY_003);
        return credentials;
    }

    public static <T>T getPrincipal(Class<T> clazz) {
        if(clazz == null)throw new EventException(SectExceptionEnum.ES_SECURITY_006);
        SecurityAuthToken authentication = getAuthentication();
        String principal = authentication.getPrincipal();
        if(StringUtils.isBlank(principal))throw new EventException(SectExceptionEnum.ES_SECURITY_004);
        try{
            return (T)(JsonUtil.stringToObj(principal,clazz).orElseThrow(()->new EventException(SectExceptionEnum.ES_SECURITY_009)));
        }catch (EventException e){
            if(e == null){
                throw new EventException(SectExceptionEnum.ES_SECURITY_009);
            }
            throw new GlobalException(e.getCode(),e.getMessage());
        }catch (ClassCastException e){
            throw new EventException(SectExceptionEnum.ES_SECURITY_009);
        } catch (Throwable throwable) {
            throw new EventException(SectExceptionEnum.ES_SECURITY_009);
        }

    }

    public static SecurityAuthToken getAuthentication(){
        if(SecurityContextHolder.getContext() == null)throw new EventException(SectExceptionEnum.ES_SECURITY_008);
        if(SecurityContextHolder.getContext().getAuthentication() == null)throw new EventException(SectExceptionEnum.ES_SECURITY_000);
        try {
            SecurityAuthToken authentication = (SecurityAuthToken) SecurityContextHolder.getContext().getAuthentication();
            return authentication;
        }catch (Exception e){
            throw new EventException(SectExceptionEnum.ES_SECURITY_009);
        }
    }
}
