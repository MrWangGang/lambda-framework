package org.lambda.framework.compliance.security;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.UUIDUtil;
import org.lambda.framework.compliance.security.container.LambdaSecurityAuthToken;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Pattern;

import static org.lambda.framework.common.enums.ConmonContract.*;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
public abstract class PrincipalFactory {

    @Resource(name = "securityAuthRedisOperation")
    private ReactiveRedisOperation securityAuthRedisOperation;
    public abstract Mono<String> getAuthToken();
    protected abstract Mono<String> fetchSubject();
    /*protected 用于隐藏这个bean里的方法，在应用层面重写*/
    public   <T extends SecurityLoginUser<?>> Mono<String> setPrincipal(T t) {
        if (t == null) {
            throw new EventException(ES_COMPLIANCE_000,"用户信息不能为空");
        }
        if (t.getId() == null) {
            throw new EventException(ES_COMPLIANCE_000,"用户ID不能为空");
        }
        String principal = JsonUtil.objToString(t);
        //为了保证一个用户只会生成一个token,token唯一性
        String keyHead = LAMBDA_SECURITY_AUTH_TOKEN_KEY + t.getId().toString();
        String keySuffix = UUIDUtil.get();
        LambdaSecurityAuthToken lambdaSecurityAuthToken = new LambdaSecurityAuthToken();
        lambdaSecurityAuthToken.setPrincipal(principal);
        lambdaSecurityAuthToken.setToken(keySuffix);
        return securityAuthRedisOperation.set(keyHead, lambdaSecurityAuthToken, LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue()).then(Mono.just(keyHead +"."+keySuffix));
    }

    public  <T extends SecurityLoginUser<?>> Mono<Void> updatePrincipal(T t) {
        return this.getAuthToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(reqKey,key).flatMap(token->{
                    String principal = JsonUtil.objToString(t);
                    token.setPrincipal(principal);
                    return securityAuthRedisOperation.update(key,token)
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_001,"无效令牌")))
                            .flatMap(flag->{
                                if(!flag)return Mono.error(new EventException(ES_COMPLIANCE_001,"更新TOKEN数据失败"));
                                return Mono.empty();
                            });
                });
            });
        });
    }

    public  <T extends SecurityLoginUser<?>> Mono<Void> revisePrincipal(T t) {
        String redisKey = LAMBDA_SECURITY_AUTH_TOKEN_KEY+t.getId();
        return securityAuthRedisOperation.get(redisKey).flatMap(token->{
            LambdaSecurityAuthToken lambdaSecurityAuthToken = JsonUtil.mapToObj((Map) token, LambdaSecurityAuthToken.class).orElseThrow(() -> new EventException(ES_COMPLIANCE_000,"令牌在提取关键部分时为空，可能是格式错误或缺少必要组件，不符合规范要求"));
            String principal = JsonUtil.objToString(t);
            lambdaSecurityAuthToken.setPrincipal(principal);
            return securityAuthRedisOperation.update(redisKey,lambdaSecurityAuthToken)
                    .flatMap(flag->{
                        if(!flag)return Mono.error(new EventException(ES_COMPLIANCE_001,"更新TOKEN数据失败"));
                        return Mono.empty();
                    });
        });
    }

    public Mono<Void> deletePrincipal() {
        return this.getAuthToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return securityAuthRedisOperation.delete(key);
            }).then();
        });
    }

    //不要使用这个方法去获取，请使用子类中的 getAuth和fetch方法去获取，这个方法获取的数据，会再次查询redis
    //这个方法只给security 用来校验使用的
    @Deprecated
    protected Mono<String> getPrincipal() {
        return this.getAuthToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(reqKey,key).flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                });
            });
        });
    }
    //不要使用这个方法去获取，请使用子类中的 getAuth和fetch方法去获取，这个方法获取的数据，会再次查询redis
    //这个方法只给security 用来校验使用的
    @Deprecated
    protected <T extends SecurityLoginUser<?>>Mono<T> getPrincipal(Class<T> clazz) {
        return this.getAuthToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(reqKey,key).flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                });
            });
        }).flatMap(e -> {
            return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_000,"用户信息不存在")));
        });
    }

    //不要使用这个方法去获取，请使用子类中的 getAuth和fetch方法去获取，这个方法获取的数据，会再次查询redis
    //这个方法只给只使用token 来校验使用的
    @Deprecated
    public   <T extends SecurityLoginUser<?>>Mono<T> getPrincipal(String rqtoken,Class<T> clazz) {
        return this.getSecurityAuthTokenKey(rqtoken).flatMap(key->{
            return this.getSecurityAuthToken(rqtoken,key).flatMap(securityAuthToken->{
                return Mono.just(securityAuthToken.getPrincipal());
            }).flatMap(e -> {
                return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_000,"用户信息不存在")));
            });
        });
    }

    public  Mono<String> fetchPrincipal() {
        return this.fetchSubject().switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"用户信息不存在")));
    }
    public  <T extends SecurityLoginUser<?>>Mono<T> fetchPrincipal2Object(Class<T> clazz) {
        return this.fetchSubject().switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_000,"用户信息不存在")))
                .flatMap(e -> {
                    return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_000,"用户信息不存在")));
                });
    }

    private Mono<String> getSecurityAuthTokenKey(String requestToken) {
        Assert.verify(requestToken,ES_COMPLIANCE_000,"令牌不能为空");
        if (!(Pattern.compile(LAMBDA_SECURITY_AUTH_TOKEN_REGEX).matcher(requestToken).matches()))
            throw new EventException(ES_COMPLIANCE_000,"令牌格式不符合规范");
        //对token进行解析
        // 在字符串中查找最后一个点的位置
        int lastDotIndex = requestToken.lastIndexOf('.');
        // 如果找到了点，则返回点之前的子字符串，否则返回原始字符串
        String token = lastDotIndex != -1 ? requestToken.substring(0, lastDotIndex) : null;
        if (StringUtils.isBlank(token)) {
            return Mono.error(new EventException(ES_COMPLIANCE_000,"令牌在提取关键部分时为空，可能是格式错误或缺少必要组件"));
        }
        return Mono.just(token);
    }


    public Mono<String> getTokenKey(String requestToken) {
        return this.getSecurityAuthTokenKey(requestToken);
    }




    private <T extends SecurityLoginUser<?>>Mono<LambdaSecurityAuthToken> getSecurityAuthToken(String rqtoken,String key) {
        Assert.verify(key,ES_COMPLIANCE_000,"令牌不能为空");
            return securityAuthRedisOperation.get(key).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_001,"无效令牌"))).flatMap(tokenBean -> {
                LambdaSecurityAuthToken lambdaSecurityAuthToken = JsonUtil.mapToObj((Map) tokenBean, LambdaSecurityAuthToken.class).orElseThrow(() -> new EventException(ES_COMPLIANCE_000,"令牌在提取关键部分时为空，可能是格式错误或缺少必要组件，不符合规范要求"));
                if (StringUtils.isBlank(lambdaSecurityAuthToken.getToken())) {
                    return Mono.error(new EventException(ES_COMPLIANCE_000,"令牌在提取关键部分时为空，可能是格式错误或缺少必要组件"));
                }
                // 查找最后一个点的位置
                int lastIndex = rqtoken.lastIndexOf('.');
                // 如果找到了点，则返回点之后的子字符串，否则返回空字符串或其他适当的默认值
                String realToken = lastIndex != -1 ? rqtoken.substring(lastIndex+1) : null;
                if (StringUtils.isBlank(realToken)) return Mono.error(new EventException(ES_COMPLIANCE_000,"令牌在提取关键部分时为空，可能是格式错误或缺少必要组件"));
                //比较token
                if (!lambdaSecurityAuthToken.getToken().equals(realToken))
                    return Mono.error(new EventException(ES_COMPLIANCE_001,"无效令牌"));
                return Mono.just(lambdaSecurityAuthToken);
            });
    }
}
