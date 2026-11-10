package org.lambda.framework.httpclient;

import org.lambda.framework.common.exception.EventException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import static org.lambda.framework.enums.HttpClientExceptionEnum.UPSTREAM_RESPONSE_FAILED;

public class HttpclientResponseHandler {
    @FunctionalInterface
    public interface ReponseVerify {
        Boolean verify(String body);
    }

    public static ExchangeFilterFunction create(ReponseVerify verifier) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            var status = response.statusCode();
            if (!status.is2xxSuccessful()) {
                String kind = status.is4xxClientError() ? "客户端错误"
                        : status.is5xxServerError() ? "服务器端错误" : "其他错误";
                return response.releaseBody().then(Mono.error(
                        new EventException(UPSTREAM_RESPONSE_FAILED, "[webclient]" + kind + ",状态码:" + status.value())));
            }
            return response.bodyToMono(String.class).flatMap(body -> {
                if (verifier == null) {
                    return Mono.error(new EventException(UPSTREAM_RESPONSE_FAILED, "[webclient]缺少响应校验钩子函数"));
                }
                if (!Boolean.TRUE.equals(verifier.verify(body))) {
                    return Mono.error(new EventException(UPSTREAM_RESPONSE_FAILED, "[webclient]响应解析失败"));
                }
                return Mono.just(response.mutate().body(body).build());
            });
        });
    }
}
