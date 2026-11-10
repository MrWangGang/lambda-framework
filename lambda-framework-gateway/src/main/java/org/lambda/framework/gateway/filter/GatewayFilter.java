package org.lambda.framework.gateway.filter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.OptionalUtil;
import org.lambda.framework.gateway.forward.RsocketRequestFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_000;

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
        String scheme = OptionalUtil.require(exchange.getRequest().getURI().getScheme(),
                () -> new EventException(ES_GATEWAY_000, "请求协议缺失"));
        if (!HTTP_SCHEME.equals(scheme) && !HTTPS_SCHEME.equals(scheme)) {
            return Mono.error(new EventException(ES_GATEWAY_000, "请求协议仅支持 HTTP 与 HTTPS"));
        }
        Route route = OptionalUtil.require(exchange.<Route>getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR),
                () -> new EventException(ES_GATEWAY_000, "该资源没有对应的路由配置"));
        URI targetRoute = OptionalUtil.require(route.getUri());
        boolean webSocket = WS_SCHEME.equals(targetRoute.getScheme());
        if (!RB_SCHEME.equals(targetRoute.getScheme()) && !webSocket) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No public MessageMapping for this route"));
        }
        String service = OptionalUtil.require(targetRoute.getHost(),
                () -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "RSocket route has no service name"));
        if (webSocket && (targetRoute.getPort() != -1 || targetRoute.getUserInfo() != null)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Use ws://service-name for this route"));
        }
        URI targetUri = OptionalUtil.require(exchange.<URI>getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR));
        GlobalFilterRoute mapping = new GlobalFilterRoute() {
            @Override
            public String host() {
                return service;
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
    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
