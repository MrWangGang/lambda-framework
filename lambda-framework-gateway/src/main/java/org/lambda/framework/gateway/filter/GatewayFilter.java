package org.lambda.framework.gateway.filter;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.lambda.framework.gateway.enums.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.*;

@Component
public class GatewayFilter implements GlobalFilter,Ordered{

    @Resource
    private RsocketRequestFactory rsocketRequestFactory;

    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI currentUri = exchange.getRequest().getURI();
        Assert.verify(currentUri.getScheme(),ES_GATEWAY_000,"未知的请求协议");
        String _scheme = currentUri.getScheme();
        if(_scheme.equals(HTTP_SCHEME) || _scheme.equals(HTTPS_SCHEME)){
            URI targetUri = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            if (RB_SCHEME.equals(targetUri.getScheme())) {
                return rsocketRequestFactory.execute(exchange,chain,targetUri);
            }
            /*if (LB_SCHEME.equals(targetUri.getScheme())) {
                return chain.filter(exchange);
            }
            throw new EventException(ES_GATEWAY_000,"scheme仅支持rb与lb");*/
            return chain.filter(exchange);
        }
        throw new EventException(ES_GATEWAY_000,"请求协议仅支持http与https");
    }
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
