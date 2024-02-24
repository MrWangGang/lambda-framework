package org.lambda.framework.compliance.security;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.common.util.sample.UUIDUtil;
import org.lambda.framework.compliance.security.container.LambdaSecurityAuthToken;
import org.lambda.framework.compliance.security.container.SecurityContract;
import org.lambda.framework.compliance.security.container.SecurityLoginUser;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;
import static org.lambda.framework.compliance.security.container.SecurityContract.TOKEN_SUFFIX;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
@Component
public class SecurityPrincipalUtil {

    @Resource(name = "securityAuthRedisOperation")
    protected ReactiveRedisOperation securityAuthRedisOperation;

    public <T extends SecurityLoginUser<?>> Mono<String> setPrincipal(T t) {
        if (t == null) {
            throw new EventException(ES_COMPLIANCE_020);
        }
        if (t.getId() == null) {
            throw new EventException(ES_COMPLIANCE_020);
        }
        String principal = JsonUtil.objToString(t);
        //为了保证一个用户只会生成一个token,token唯一性
        String keyHead = SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY + MD5Util.hash(t.getId().toString()) + ".";
        String keySuffix = MD5Util.hash(t.getId() +"."+SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_SALT + "." + UUIDUtil.get());
        LambdaSecurityAuthToken<T> lambdaSecurityAuthToken = new LambdaSecurityAuthToken<T>();
        lambdaSecurityAuthToken.setPrincipal(principal);
        lambdaSecurityAuthToken.setToken(keySuffix);
        return securityAuthRedisOperation.set(keyHead + TOKEN_SUFFIX, lambdaSecurityAuthToken, SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue()).then(Mono.just(keyHead + keySuffix));
    }

    public <T extends SecurityLoginUser<?>> Mono<Void> updatePrincipal(T t) {
        return this.getServerRequestToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(key).flatMap(token->{
                    token.setPrincipal(JsonUtil.objToString(t));
                    return securityAuthRedisOperation.update(key,token)
                            .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_022)))
                            .flatMap(flag->{
                                if(!flag)return Mono.error(new EventException(ES_COMPLIANCE_022));
                                return Mono.just(token.getPrincipal());
                            });
                });
            }).then();
        });
    }

    public Mono<Void> deletePrincipal() {
        return this.getServerRequestToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return securityAuthRedisOperation.delete(key);
            }).then();
        });
    }

    public Mono<String> getPrincipal() {
        return this.getServerRequestToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(key).flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                });
            });
        });
    }
    public <T extends SecurityLoginUser<?>>Mono<T> getPrincipal2Object(Class<T> clazz) {
        return this.getPrincipal().flatMap(e -> {
            return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_023)));
        }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
    }


    protected static Mono<ServerHttpRequest> getServerHttpRequest() {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(ServerWebExchange.class).getRequest());
    }
    protected Mono<String> getServerRequestToken(){
        return getServerHttpRequest().flatMap(e -> {
            List<String> headers = e.getHeaders().get(SecurityContract.AUTH_TOKEN_NAMING);
            if(headers == null || headers.isEmpty() || headers.get(0) == null){
                return Mono.error(new EventException(ES_COMPLIANCE_021));
            }
            return Mono.just(e.getHeaders().get(SecurityContract.AUTH_TOKEN_NAMING).get(0));
        }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
    }

    protected Mono<String> getSecurityAuthTokenKey(String requestToken) {
        Assert.verify(requestToken,ES_COMPLIANCE_021);
        if (!(Pattern.compile(SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_REGEX).matcher(requestToken).matches()))
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
    protected <T extends SecurityLoginUser<?>>Mono<LambdaSecurityAuthToken<?>> getSecurityAuthToken(String key) {
        Assert.verify(key,ES_COMPLIANCE_021);
        return this.getServerRequestToken().flatMap(rqtoken->{
            return this.securityAuthRedisOperation.get(key).flatMap(tokenBean -> {
                LambdaSecurityAuthToken<?> lambdaSecurityAuthToken = JsonUtil.mapToObj((Map) tokenBean, LambdaSecurityAuthToken.class).orElseThrow(() -> new EventException(ES_COMPLIANCE_021));
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
