package org.lambda.framework.gateway.adapter;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.lambda.framework.gateway.enums.GatewayExceptionEnum.ES_GATEWAY_016;

@Configuration
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class WebGlobalResponseHandler  {

    private static Mono<ResponseTemplete> methodForParams() {
        return Mono.empty();
    }
    private static MethodParameter METHOD_PARAMETER;

    static {
        try {
            // 获得 METHOD_PARAMETER 。其中 -1 表示 `#methodForParams()` 方法的返回值
            METHOD_PARAMETER = new MethodParameter(
                    WebGlobalResponseHandler.class.getDeclaredMethod("methodForParams"), -1);
        } catch (NoSuchMethodException e) {
            throw new EventException(ES_GATEWAY_016);
        }
    }

    @Bean
    public ResponseBodyResultHandler responseWrapper(ServerCodecConfigurer serverCodecConfigurer,
                                                    RequestedContentTypeResolver requestedContentTypeResolver) {
        return new ResponseBodyResultHandler(serverCodecConfigurer.getWriters(), requestedContentTypeResolver) {
            @Override
            public boolean supports(HandlerResult result) {
                return true;
            }
            @Override
            public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
                Object returnValue = result.getReturnValue();
                Object body;
                // <1.1>  处理返回结果为 Mono 的情况
                if (returnValue instanceof Mono) {
                    body = ((Mono<Object>) result.getReturnValue())
                            .map(e-> new ResponseTemplete(e))
                            .defaultIfEmpty(new ResponseTemplete());
                    //  <1.2> 处理返回结果为 Flux 的情况
                } else if (returnValue instanceof Flux) {
                    body = ((Flux<Object>) result.getReturnValue())
                            .collectList()
                            .map(e-> new ResponseTemplete(e))
                            .defaultIfEmpty(new ResponseTemplete());
                    //  <1.3> 处理结果为其它类型
                } else {
                    //不允许其他类型返回
                    //throw new EventException(ES_WEB_002);
                    body = result.getReturnValue();
                }
                return writeBody(body, METHOD_PARAMETER, exchange);
            }
        };
    }
}