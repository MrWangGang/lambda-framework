package org.lambda.framework.security.manger;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.ConmonContract.*;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.*;

@Component
//security 的过滤器 需要特殊处理token和principal 需要从数据库中去获取，再验证，保证值是最新的 并且是最安全的
class SecurityPrincipalFactory extends PrincipalFactory {
    @Override
    public Mono<String> getAuthToken(){
        return getRequest(AUTHTOKEN_STASH_NAMING);
    }
    Mono<String> principal(){
        return super.getPrincipal();
    }
    @Override
    protected Mono<String> fetchSubject() {
        return getRequest(PRINCIPAL_STASH_NAMING);
    }

    private  Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView ->{
                    ServerWebExchange serverWebExchange = contextView.get(ServerWebExchange.class);
                    Assert.verify(serverWebExchange,ES_SECURITY_000,"缺少身份认证信息");
                    Object token = serverWebExchange.getAttributes().get(key);
                    Assert.verify(token,ES_SECURITY_000,"该资源需要令牌才能访问");
                    String value = token.toString();
                   return value;
                })
                .switchIfEmpty(Mono.error(new EventException(ES_SECURITY_000,"该资源需要令牌才能访问")));
    }
}
