package org.lambda.framework.gateway.filter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;

@Component
public class GatewayFilter implements GlobalFilter,Ordered{

    public static interface GlobalFilterRoute {
        String host();
        String path();
        default Map<String, Object> metadata() { return Map.of(); }
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

            boolean webSocket = WS_SCHEME.equals(targetRoute.getScheme());
            if (!RB_SCHEME.equals(targetRoute.getScheme()) && !webSocket) {
                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No public MessageMapping for this route"));
            }
            if (webSocket && (targetRoute.getHost() == null || targetRoute.getPort() != -1
                    || targetRoute.getUserInfo() != null)) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Use ws://service-name for this route"));
            }
            URI targetUri = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            if(targetUri == null){
                throw new EventException(ES_GATEWAY_000,"该资源没有对应的路由信息");
            }
            GlobalFilterRoute mapping = new GlobalFilterRoute() {
                @Override
                public String host() {
                    return targetRoute.getHost();
                }

                @Override
                public String path() {
                    return targetUri.getPath();
                }

                @Override
                public Map<String, Object> metadata() {
                    return route.getMetadata();
                }
            };
            return webSocket ? rsocketRequestFactory.executeWebSocket(exchange, mapping)
                    : rsocketRequestFactory.execute(exchange, chain, mapping);
        }
        throw new EventException(ES_GATEWAY_000,"请求协议仅支持http与https");
    }
    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
