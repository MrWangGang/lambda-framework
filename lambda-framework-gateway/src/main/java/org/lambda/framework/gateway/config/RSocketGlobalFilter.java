package org.lambda.framework.gateway.config;

import jakarta.annotation.Resource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RSocketGlobalFilter implements GlobalFilter {
    @Resource
    private
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求的 URL
        String requestUrl = exchange.getRequest().getURI().toString();
        // 如果 URL 以 rsocket:// 开头，则进行 RSocket 转发
        if (requestUrl.startsWith("rsocket://")) {
            // 在这里添加 RSocket 转发的逻辑
            // 你可能需要使用 RSocket 客户端来建立连接并发送请求
            //exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);

            return exchange.getResponse().setComplete();
        }
        // 如果不是 rsocket:// 开头的 URL，则继续执行后续过滤器链
        return chain.filter(exchange);
    }
}
