package org.lambda.framework.httpclient;

import org.lambda.framework.common.exception.EventException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import static org.lambda.framework.httpclient.enums.HttpclientExceptionEnum.ES_HTTPCLIENT_000;

public class HttpclientResponseHandler {
    public ExchangeFilterFunction exchangeFilterFunction() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is2xxSuccessful()) {
                return Mono.just(clientResponse);
            } else {
                // 处理错误响应
                if (clientResponse.statusCode().is4xxClientError()) {
                    return Mono.error(new EventException(ES_HTTPCLIENT_000, "[webclient]客户端错误,状态码:" + clientResponse.statusCode().value()));
                } else if (clientResponse.statusCode().is5xxServerError()) {
                    return Mono.error(new EventException(ES_HTTPCLIENT_000, "[webclient]服务器端错误,状态码:" + clientResponse.statusCode().value()));
                } else if (clientResponse.statusCode().value() == 401) {
                    return Mono.error(new EventException(ES_HTTPCLIENT_000, "[webclient]未授权访问,状态码:" + clientResponse.statusCode().value()));
                } else if (clientResponse.statusCode().value() == 403) {
                    return Mono.error(new EventException(ES_HTTPCLIENT_000, "[webclient]禁止访问,状态码:" + clientResponse.statusCode().value()));
                } else {
                    return Mono.error(new EventException(ES_HTTPCLIENT_000, "[webclient]其他错误,状态码:" + clientResponse.statusCode().value()));
                }
            }
        });
    }
}
