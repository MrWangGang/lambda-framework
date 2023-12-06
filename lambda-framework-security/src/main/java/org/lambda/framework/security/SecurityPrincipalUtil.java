package org.lambda.framework.security;

import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.common.util.sample.UUIDUtil;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.container.LambdaSecurityAuthToken;
import org.lambda.framework.security.container.SecurityAuthToken;
import org.lambda.framework.security.container.SecurityLoginUser;
import org.lambda.framework.security.contract.SecurityContract;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.lambda.framework.security.contract.SecurityContract.AUTH_TOKEN_NAMING;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.*;

/**
 * @description: 获取spring secutiy中的principal
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 3:28
 **/
@Component
public class SecurityPrincipalUtil {

    private static String TOKEN_SUFFIX = "prefix";

    @Resource(name = "securityAuthRedisOperation")
    private ReactiveRedisOperation securityAuthRedisOperation;

    public Mono<String> getPrincipal() {
        return getServerHttpRequest().flatMap(e -> {
            List headers = e.getHeaders().get(SecurityContract.AUTH_TOKEN_NAMING);
            if(headers == null || headers.size() == 0 || headers.get(0) == null){
                return Mono.error(new EventException(ES_SECURITY_003));
            }
            return Mono.just(e.getHeaders().get(AUTH_TOKEN_NAMING).get(0));
        }).switchIfEmpty(Mono.error(new EventException(ES_SECURITY_003))).flatMap(e -> {
            if (!(Pattern.compile(SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_REGEX).matcher(e).matches()))
                throw new EventException(SecurityExceptionEnum.ES_SECURITY_007);
            //对token进行解析
            // 在字符串中查找最后一个点的位置
            int lastDotIndex = e.lastIndexOf('.');
            // 如果找到了点，则返回点之前的子字符串，否则返回原始字符串
            String token = lastDotIndex != -1 ? e.substring(0, lastDotIndex + 1) : null;
            if (StringUtils.isBlank(token)) {
                return Mono.error(new EventException(ES_SECURITY_003));
            }
            token = token + TOKEN_SUFFIX;
            return securityAuthRedisOperation.get(token).flatMap(tokenBean -> {
                LambdaSecurityAuthToken lambdaSecurityAuthToken = JsonUtil.mapToObj((Map) tokenBean, LambdaSecurityAuthToken.class).orElseThrow(() -> new EventException(ES_SECURITY_003));
                if (StringUtils.isBlank(lambdaSecurityAuthToken.getToken())) {
                    return Mono.error(new EventException(ES_SECURITY_003));
                }
                // 查找最后一个点的位置
                int lastIndex = e.lastIndexOf('.');
                // 如果找到了点，则返回点之后的子字符串，否则返回空字符串或其他适当的默认值
                String realToken = lastIndex != -1 ? e.substring(lastIndex + 1) : null;
                if (StringUtils.isBlank(realToken)) return Mono.error(new EventException(ES_SECURITY_003));
                //比较token
                if (!lambdaSecurityAuthToken.getToken().equals(realToken))
                    return Mono.error(new EventException(ES_SECURITY_003));
                return Mono.just(lambdaSecurityAuthToken.getPrincipal());
            }).switchIfEmpty(Mono.error(new EventException(ES_SECURITY_003)));
        });
    }
    public <T extends SecurityLoginUser>Mono<T> getPrincipal2Object(Class<T> clazz) {
        return this.getPrincipal().flatMap(e -> {
            return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_SECURITY_004)));
        }).switchIfEmpty(Mono.error(new EventException(ES_SECURITY_004)));
    }

    ;

    public <T extends SecurityLoginUser> Mono<String> setPrincipal(T t) {
        if (t == null) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_002);
        }
        if (t.getId() == null) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_002);
        }
        String principal = JsonUtil.objToString(t);
        String keyHead = SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_KEY + MD5Util.hash(t.getId().toString()) + ".";
        String keySuffix = MD5Util.hash(principal + "." + SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_SALT + "." + UUIDUtil.get());
        LambdaSecurityAuthToken lambdaSecurityAuthToken = new LambdaSecurityAuthToken();
        lambdaSecurityAuthToken.setPrincipal(JsonUtil.objToString(t));
        lambdaSecurityAuthToken.setToken(keySuffix);
        return securityAuthRedisOperation.set(keyHead + TOKEN_SUFFIX, lambdaSecurityAuthToken, SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND.longValue()).then(Mono.just(keyHead + keySuffix));
    }

    public Mono<Void> deletePrincipal() {
        return getServerHttpRequest().flatMap(e -> {
            return Mono.just(e.getHeaders().get(AUTH_TOKEN_NAMING).get(0));
        }).switchIfEmpty(Mono.error(new EventException(ES_SECURITY_003))).flatMap(e -> {
            return securityAuthRedisOperation.delete(e);
        }).then(Mono.empty());
    }

    public static Mono<ServerHttpRequest> getServerHttpRequest() {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(ServerWebExchange.class).getRequest());
    }


    //这些方法不是响应式的可能会出问题，不要使用
    @Deprecated
    public static String getCredentials() {
        SecurityAuthToken authentication = getAuthentication();
        String credentials = authentication.getCredentials();
        if (StringUtils.isBlank(credentials)) throw new EventException(ES_SECURITY_003);
        return credentials;
    }

    @Deprecated
    public static <T> T getPrincipal(Class<T> clazz) {
        if (clazz == null) throw new EventException(ES_SECURITY_006);
        SecurityAuthToken authentication = getAuthentication();
        String principal = authentication.getPrincipal();
        if (StringUtils.isBlank(principal)) throw new EventException(ES_SECURITY_004);
        try {
            return (T) (JsonUtil.stringToObj(principal, clazz).orElseThrow(() -> new EventException(SecurityExceptionEnum.ES_SECURITY_009)));
        } catch (EventException e) {
            if (e == null) {
                throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
            }
            throw new GlobalException(e.getCode(), e.getMessage());
        } catch (ClassCastException e) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        } catch (Throwable throwable) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        }

    }

    @Deprecated
    public static SecurityAuthToken getAuthentication() {
        if (SecurityContextHolder.getContext() == null) throw new EventException(SecurityExceptionEnum.ES_SECURITY_008);
        if (SecurityContextHolder.getContext().getAuthentication() == null)
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_000);
        try {
            SecurityAuthToken authentication = (SecurityAuthToken) SecurityContextHolder.getContext().getAuthentication();
            return authentication;
        } catch (Exception e) {
            throw new EventException(SecurityExceptionEnum.ES_SECURITY_009);
        }
    }
}
