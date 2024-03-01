package org.lambda.framework.gateway.config;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.loadbalance.config.RSocketLoadbalance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.lambda.framework.gateway.config.GatewayContract.*;
import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class RSocketGlobalFilter implements GlobalFilter {
    @Resource
    private RSocketLoadbalance rsocketLoanbalance;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求的 URL
        String serviceName = exchange.getAttribute(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
        URI targetUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        Assert.verify(serviceName,ES_GATEWAY_000);
        Assert.verify(targetUri,ES_GATEWAY_001);
        Assert.verify(targetUri.getScheme(),ES_GATEWAY_001);
        ServerHttpRequest request = exchange.getRequest();
        URI currentUri = exchange.getRequest().getURI();
        String _scheme = currentUri.getScheme();
        Assert.verify(currentUri.getScheme(),ES_GATEWAY_002);
        //如果请求网关的协议是http开头的
        if(_scheme.equals(HTTP_SCHEME) || _scheme.equals(HTTPS_SCHEME)){
            // 如果 目标URL 以 rsocket:// 开头，则进行 RSocket 转发
            if (RSOCKET_SCHEME.equals(targetUri.getScheme())) {
                Flux<DataBuffer> body = exchange.getRequest().getBody();
                AtomicReference<byte[]> atomicReference = new AtomicReference<>();
                /**
                 * 获取客户端请求的数据，body体
                 */
                body.subscribe(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    atomicReference.set(bytes);
                    // 在这里添加 RSocket 转发的逻辑
                    // 你可能需要使用 RSocket 客户端来建立连接并发送请求
                    //exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                    RSocketRequester requester = rsocketLoanbalance.build(serviceName);
                    RSocketRequester.RequestSpec spec = requester.route(currentUri.getPath());
                    spec.retrieveMono(String.class);
                });
                return exchange.getResponse().setComplete();
            }
            // 如果 目标URL 不是 rsocket:// 开头的 URL，则继续执行后续过滤器链
            return chain.filter(exchange);
        }
        throw new EventException(ES_GATEWAY_002);
    }
}
