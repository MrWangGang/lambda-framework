package org.lambda.framework.httpclient;

import io.netty.channel.ChannelOption;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.HttpClientExceptionEnum;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public final class ReactiveHttpClient {
    private static final int DEFAULT_RESPONSE_BYTES = 256 * 1024;
    private static final byte[] EMPTY_BODY = new byte[0];

    private final URI origin;
    private final Duration timeout;
    private final WebClient client;

    public ReactiveHttpClient(URI origin, Duration connectTimeout, Duration timeout) {
        this(origin, connectTimeout, timeout, DEFAULT_RESPONSE_BYTES);
    }

    public ReactiveHttpClient(URI origin, Duration connectTimeout, Duration timeout, int maxResponseBytes) {
        if (!("https".equals(origin.getScheme()) || "http".equals(origin.getScheme())) || origin.getHost() == null
                || origin.getRawUserInfo() != null || origin.getRawQuery() != null || origin.getRawFragment() != null
                || !(origin.getRawPath().isEmpty() || "/".equals(origin.getRawPath()))) {
            throw new IllegalArgumentException("HTTP client requires an origin without a path, query or fragment");
        }
        long connectMillis = connectTimeout.toMillis();
        if (connectMillis < 1 || timeout.isNegative() || timeout.isZero() || maxResponseBytes < 1) {
            throw new IllegalArgumentException("HTTP client timeouts and response limit must be positive");
        }
        this.origin = origin;
        this.timeout = timeout;
        var transport = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectMillis))
                .responseTimeout(timeout)
                .disableRetry(true);
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(transport))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(maxResponseBytes))
                .build();
    }

    public <T> Mono<T> retrieve(HttpMethod method, URI target, HttpHeaders headers, Class<T> responseType) {
        return retrieve(method, target, headers, EMPTY_BODY, responseType);
    }

    public <T> Mono<T> retrieve(HttpMethod method, URI target, HttpHeaders headers, byte[] body, Class<T> responseType) {
        return exchange(method, target, headers, body, response -> {
            if (response.statusCode().is2xxSuccessful()) return response.bodyToMono(responseType);
            return Mono.error(new EventException(HttpClientExceptionEnum.UPSTREAM_RESPONSE_FAILED,
                    "上游 HTTP 状态码 " + response.statusCode().value()));
        });
    }

    public <T> Mono<T> exchange(HttpMethod method, URI target, HttpHeaders headers, Function<ClientResponse, Mono<T>> reader) {
        return exchange(method, target, headers, EMPTY_BODY, reader);
    }

    public <T> Mono<T> exchange(HttpMethod method, URI target, HttpHeaders headers, byte[] body,
                                Function<ClientResponse, Mono<T>> reader) {
        return Mono.defer(() -> {
            if (target.isAbsolute() || target.getRawAuthority() != null || target.getRawFragment() != null
                    || !target.getRawPath().startsWith("/") || target.getRawPath().startsWith("//")) {
                return Mono.error(new IllegalArgumentException("HTTP request requires an origin-relative path"));
            }
            var request = client.method(method).uri(origin.resolve(target)).headers(outbound -> outbound.addAll(headers));
            return (body.length == 0 ? request : request.bodyValue(body)).exchangeToMono(reader);
        }).timeout(timeout).onErrorMap(
                failure -> failure instanceof WebClientRequestException || failure instanceof TimeoutException,
                failure -> new EventException(HttpClientExceptionEnum.UPSTREAM_RESPONSE_FAILED));
    }
}
