package org.lambda.framework.web.adapter;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.support.SecurityStash;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;

@Component
@Order(Integer.MAX_VALUE)
//必须保证这个过滤器在最后执行
public class WebGlobalSecurityFileter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Object principal = exchange.getAttributes().get(PRINCIPAL_STASH_NAMING);
        Object authtoken = exchange.getAttributes().get(AUTHTOKEN_STASH_NAMING);
        SecurityStash securityStash = SecurityStash.builder().build();
        if(Assert.verify(principal)){
            securityStash.setPrincipal(principal.toString());
        }
        if(Assert.verify(authtoken)){
            securityStash.setAuthToken(authtoken.toString());
        }
        return chain.filter(exchange).contextWrite(Context.of(SecurityStash.class,securityStash));
    }
}
