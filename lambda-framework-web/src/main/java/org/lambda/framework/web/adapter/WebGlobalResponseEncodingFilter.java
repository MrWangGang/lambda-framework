package org.lambda.framework.web.adapter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.lambda.framework.web.enums.GlobalResponseContentType.APPLICATION_JSON_UTF8;

@Component
@Order(-1) // 设置较高的优先级，确保在其他过滤器之前执行
public class WebGlobalResponseEncodingFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().getHeaders().setContentType(APPLICATION_JSON_UTF8);
        return chain.filter(exchange);
    }
}
