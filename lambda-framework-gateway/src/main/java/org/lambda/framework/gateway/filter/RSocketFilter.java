package org.lambda.framework.gateway.filter;

import jakarta.annotation.Resource;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lambda.framework.gateway.config.GatewayContract.RSOCKET_SCHEME;

@Component
public class RSocketFilter implements GlobalFilter,Ordered{
    @Resource
    private RsocketRequestFactory rsocketRequestFactory;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return rsocketRequestFactory.execute(exchange,chain,RSOCKET_SCHEME,(rSocketLoadbalance,host,port,path)->{
            return rSocketLoadbalance.build(host,port,path);
        });
    }
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
