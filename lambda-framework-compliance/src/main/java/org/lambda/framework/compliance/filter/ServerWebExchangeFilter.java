package org.lambda.framework.compliance.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;


//将ServerWebExchange存入context，使得mono操作链里的任何地方都可以去获取
@Component
public class ServerWebExchangeFilter  implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(contextView -> {
            // 将 ServerWebExchange 放入 Context 中
            Context context = Context.of(ServerWebExchange.class, exchange);
            // 将修改后的 Context 传递给下游方法
            return chain.filter(exchange).contextWrite(context);
        });
    }
}
