package org.lambda.framework.security.manger;


import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.manger.support.SecurityAuthToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Map;

import static org.lambda.framework.common.enums.ConmonContract.*;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.*;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2021-10-19 下午 1:18
 **/
public abstract class SecurityAuthManager implements SecurityAuthVerify {
    @Resource(name = "securityAuthRedisOperation")
    private ReactiveRedisOperation securityAuthRedisOperation;

    @Resource
    private SecurityPrincipalFactory securityPrincipalFactory;

    @Override
    public Mono<SecurityAuthToken> authenticate(AuthorizationContext authorizationContext) {
        //converter
        ServerWebExchange exchange = authorizationContext.getExchange();
        String authToken = exchange.getRequest().getHeaders().getFirst(AUTH_TOKEN_NAMING);
        Assert.verify(authToken,ES_SECURITY_000,"该资源需要令牌才能访问");
        Map<String, Object> map = exchange.getAttributes();
        Assert.verify(map,ES_SECURITY_000,"缺少身份认证信息");
        exchange.getAttributes().put(AUTHTOKEN_STASH_NAMING,authToken);
        if(StringUtils.isBlank(authToken))throw new EventException(ES_SECURITY_000,"该资源需要令牌才能访问");
        return securityPrincipalFactory.principal().flatMap(principal -> {
            if(StringUtils.isBlank(principal.toString()))return Mono.error(new EventException(ES_SECURITY_000,"用户信息不存在"));
            //更新SecurityContext中的Authentication信息
            if(verify(principal) == false) return Mono.error(new EventException(ES_SECURITY_000,"身份认证失败"));
               SecurityAuthToken securityAuthToken = SecurityAuthToken.builder().principal(principal.toString()).credentials(authToken).authenticated(true).build();
               return Mono.just(securityAuthToken).flatMap(token->{
                                return putToken(token).map(e->{
                                    return token;
                                });
                            });
        });
    }

    private  Mono<ContextView> putToken(SecurityAuthToken token) {
        return Mono.deferContextual(Mono::just)
                .map(contextView ->{
                    ServerWebExchange serverWebExchange = contextView.get(ServerWebExchange.class);
                    Assert.verify(serverWebExchange,ES_SECURITY_000,"缺少身份认证信息");
                    serverWebExchange.getAttributes().put(PRINCIPAL_STASH_NAMING,token.getPrincipal());
                    serverWebExchange.getAttributes().put(AUTHTOKEN_STASH_NAMING,token.getCredentials());
                    return contextView;
                })
                .switchIfEmpty(Mono.error(new EventException(ES_SECURITY_000,"缺少身份认证信息")));
    }
}
