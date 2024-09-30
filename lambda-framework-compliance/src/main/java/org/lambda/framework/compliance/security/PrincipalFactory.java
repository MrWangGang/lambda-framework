package org.lambda.framework.compliance.security;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.common.util.sample.UUIDUtil;
import org.lambda.framework.compliance.security.container.LambdaSecurityAuthToken;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
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
            throw new EventException(ES_COMPLIANCE_020);
        }
        if (t.getId() == null) {
            throw new EventException(ES_COMPLIANCE_020);
        }
        String principal = JsonUtil.objToString(t);
        //为了保证一个用户只会生成一个token,token唯一性
        String keyHead = LAMBDA_SECURITY_AUTH_TOKEN_KEY + MD5Util.hash(t.getId().toString()) + ".";
        String keySuffix = MD5Util.hash(t.getId() +"."+LAMBDA_SECURITY_AUTH_TOKEN_SALT + "." + UUIDUtil.get());
        LambdaSecurityAuthToken lambdaSecurityAuthToken = new LambdaSecurityAuthToken();
        lambdaSecurityAuthToken.setPrincipal(principal);
        lambdaSecurityAuthToken.setToken(keySuffix);
        return securityAuthRedisOperation.set(keyHead + TOKEN_SUFFIX, lambdaSecurityAuthToken, LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue()).then(Mono.just(keyHead + keySuffix));
    }

    public  <T extends SecurityLoginUser<?>> Mono<Void> updatePrincipal(T t) {
        return this.getAuthToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(key).flatMap(token->{
                    String principal = JsonUtil.objToString(t);
                    token.setPrincipal(principal);
                    return securityAuthRedisOperation.update(key,token,LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue())
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_022)))
                            .flatMap(flag->{
                                if(!flag)return Mono.error(new EventException(ES_COMPLIANCE_022));
                                return Mono.empty();
                            });
                });
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
                return this.getSecurityAuthToken(key).flatMap(securityAuthToken->{
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
                return this.getSecurityAuthToken(key).flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                });
            });
        }).flatMap(e -> {
            return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_019)));
        }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
    }

    //不要使用这个方法去获取，请使用子类中的 getAuth和fetch方法去获取，这个方法获取的数据，会再次查询redis
    //这个方法只给只使用token 来校验使用的
    @Deprecated
    public   <T extends SecurityLoginUser<?>>Mono<T> getPrincipal(String token,Class<T> clazz) {
        return this.getSecurityAuthTokenKey(token).flatMap(key->{
            return this.getSecurityAuthToken(key).flatMap(securityAuthToken->{
                return Mono.just(securityAuthToken.getPrincipal());
            }).flatMap(e -> {
                return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_019)));
            }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
        });
    }

    public  Mono<String> fetchPrincipal() {
        return this.fetchSubject().switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
    }
    public  <T extends SecurityLoginUser<?>>Mono<T> fetchPrincipal2Object(Class<T> clazz) {
        return this.fetchSubject().switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)))
                .flatMap(e -> {
                    return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_019)));
                }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
    }

    private Mono<String> getSecurityAuthTokenKey(String requestToken) {
        Assert.verify(requestToken,ES_COMPLIANCE_021);
        if (!(Pattern.compile(LAMBDA_SECURITY_AUTH_TOKEN_REGEX).matcher(requestToken).matches()))
            throw new EventException(ES_COMPLIANCE_024);
        //对token进行解析
        // 在字符串中查找最后一个点的位置
        int lastDotIndex = requestToken.lastIndexOf('.');
        // 如果找到了点，则返回点之前的子字符串，否则返回原始字符串
        String token = lastDotIndex != -1 ? requestToken.substring(0, lastDotIndex + 1) : null;
        if (StringUtils.isBlank(token)) {
            return Mono.error(new EventException(ES_COMPLIANCE_021));
        }
        token = token + TOKEN_SUFFIX;
        return Mono.just(token);
    }
    private <T extends SecurityLoginUser<?>>Mono<LambdaSecurityAuthToken> getSecurityAuthToken(String key) {
        Assert.verify(key,ES_COMPLIANCE_021);
        return this.getAuthToken().flatMap(rqtoken->{
            return securityAuthRedisOperation.get(key).flatMap(tokenBean -> {
                LambdaSecurityAuthToken lambdaSecurityAuthToken = JsonUtil.mapToObj((Map) tokenBean, LambdaSecurityAuthToken.class).orElseThrow(() -> new EventException(ES_COMPLIANCE_021));
                if (StringUtils.isBlank(lambdaSecurityAuthToken.getToken())) {
                    return Mono.error(new EventException(ES_COMPLIANCE_021));
                }
                // 查找最后一个点的位置
                int lastIndex = rqtoken.lastIndexOf('.');
                // 如果找到了点，则返回点之后的子字符串，否则返回空字符串或其他适当的默认值
                String realToken = lastIndex != -1 ? rqtoken.substring(lastIndex + 1) : null;
                if (StringUtils.isBlank(realToken)) return Mono.error(new EventException(ES_COMPLIANCE_021));
                //比较token
                if (!lambdaSecurityAuthToken.getToken().equals(realToken))
                    return Mono.error(new EventException(ES_COMPLIANCE_021));
                return Mono.just(lambdaSecurityAuthToken);
            }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
        });
    }
}
