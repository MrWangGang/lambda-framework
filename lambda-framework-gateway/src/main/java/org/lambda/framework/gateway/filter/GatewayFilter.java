package org.lambda.framework.gateway.filter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;

@Component
public class GatewayFilter implements GlobalFilter,Ordered{

    public static interface GlobalFilterRoute {
        String host();
        String path();
    }

    @Resource
    private RsocketRequestFactory rsocketRequestFactory;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI currentUri = exchange.getRequest().getURI();
        Assert.verify(currentUri.getScheme(),ES_GATEWAY_000,"未知的请求协议");
        String _scheme = currentUri.getScheme();
        if(_scheme.equals(HTTP_SCHEME) || _scheme.equals(HTTPS_SCHEME)){
            Route route = (Route)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if(route == null){
                throw new EventException(ES_GATEWAY_000,"该资源没有对应的路由配置信息");
            }
            URI targetRoute = route.getUri();
            if(targetRoute == null){
                throw new EventException(ES_GATEWAY_000,"该资源没有对应的路由配置信息");
            }

            URI targetUri = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            if(targetUri == null){
                throw new EventException(ES_GATEWAY_000,"该资源没有对应的路由信息");
            }

            if (RB_SCHEME.equals(targetUri.getScheme())) {
                return rsocketRequestFactory.execute(exchange, chain, new GlobalFilterRoute() {
                    @Override
                    public String host() {
                        return targetRoute.getHost();
                    }

                    @Override
                    public String path() {
                        return targetUri.getPath();
                    }
                });
            }

            if (LB_SCHEME.equals(targetUri.getScheme())) {
                return chain.filter(exchange);
            }
            throw new EventException(ES_GATEWAY_000,"scheme仅支持rb与lb");
        }
        throw new EventException(ES_GATEWAY_000,"请求协议仅支持http与https");
    }
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
