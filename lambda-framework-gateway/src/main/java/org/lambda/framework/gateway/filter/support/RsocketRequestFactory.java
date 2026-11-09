package org.lambda.framework.gateway.filter.support;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.gateway.filter.GatewayFilter;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.lambda.framework.gateway.enums.GatewayContract.*;

@Component
public class RsocketRequestFactory {
    private static final byte[] EMPTY_BODY = new byte[0];
    private static final byte[] EMPTY_RESPONSE = encode(new ResponseTemplete());
    private static final String DATA_START = new String(EMPTY_RESPONSE, StandardCharsets.UTF_8)
            .replaceFirst("}$", "") + ",\"data\":";
    private static final byte[] OBJECT_START = DATA_START.getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARRAY_START = (DATA_START + "[").getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARRAY_END = {']', '}'};
    private static final JsonFactory UTF8_JSON = JsonUtil.getJsonFactory().getFactory().copy()
            .disable(JsonFactory.Feature.CHARSET_DETECTION);

    private final RSocketLoadbalance loadbalance;
    private final PrincipalStash principalStash;
    private final int maxRequestBytes;
    private final int maxResponseBytes;
    private final int maxResponseItems;
    private final Duration requestTimeout;
    private final StringDecoder lineDecoder;

    public RsocketRequestFactory(RSocketLoadbalance loadbalance, PrincipalStash principalStash,
                                 @Value("${lambda.gateway.rsocket.max-request-bytes:1048576}") int maxRequestBytes,
                                 @Value("${lambda.gateway.rsocket.max-response-bytes:4194304}") int maxResponseBytes,
                                 @Value("${lambda.gateway.rsocket.max-response-items:512}") int maxResponseItems,
                                 @Value("${lambda.gateway.rsocket.request-timeout:15s}") Duration requestTimeout) {
        if (maxRequestBytes < 1 || maxResponseBytes < ARRAY_START.length + ARRAY_END.length
                || maxResponseItems < 1 || requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("Invalid HTTP to RSocket bridge limits");
        }
        this.loadbalance = loadbalance;
        this.principalStash = principalStash;
        this.maxRequestBytes = maxRequestBytes;
        this.maxResponseBytes = maxResponseBytes;
        this.maxResponseItems = maxResponseItems;
        this.requestTimeout = requestTimeout;
        this.lineDecoder = StringDecoder.allMimeTypes(List.of("\n"), true);
        this.lineDecoder.setMaxInMemorySize(maxRequestBytes);
        this.lineDecoder.setDefaultCharset(StandardCharsets.UTF_8);
    }

    public Mono<Void> execute(ServerWebExchange exchange, GatewayFilterChain chain,
                              GatewayFilter.GlobalFilterRoute route) {
        return Mono.defer(() -> forward(exchange, route));
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilter.GlobalFilterRoute route) {
        var request = exchange.getRequest();
        if (request.getMethod() != HttpMethod.POST) {
            return Mono.error(new MethodNotAllowedException(request.getMethod(), List.of(HttpMethod.POST)));
        }
        if (!request.getQueryParams().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a request DTO in the POST body"));
        }
        HttpHeaders headers = request.getHeaders();
        RequestFormat format = resolveFormat(route.metadata(), headers);
        String model = format.model();
        String echo = format.echo();
        if (!RSOCKET_MODEL_REQUEST_RESPONSE.equals(model) && !RSOCKET_MODEL_REQUEST_STREAM.equals(model)
                && !RSOCKET_MODEL_REQUEST_CHANNEL.equals(model)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RSocket-Model"));
        }
        if (!RSOCKET_ECHO_OBJECT.equals(echo) && !RSOCKET_ECHO_CHAR.equals(echo) && !RSOCKET_ECHO_VOID.equals(echo)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RSocket-Echo"));
        }
        MediaType contentType = format.contentType();
        ServerHttpResponse response = exchange.getResponse();
        Class<?> responseType = RSOCKET_ECHO_OBJECT.equals(echo) ? Object.class : String.class;
        if (RSOCKET_MODEL_REQUEST_CHANNEL.equals(model)) {
            if (!MediaType.APPLICATION_NDJSON.isCompatibleWith(contentType) || RSOCKET_ECHO_VOID.equals(echo)) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "request/channel requires UTF-8 application/x-ndjson and an object or char reply"));
            }
            Flux<byte[]> frames = lineDecoder.decode(request.getBody(), ResolvableType.forClass(String.class),
                            MediaType.APPLICATION_NDJSON, Map.of())
                    .filter(line -> !line.isBlank())
                    .map(line -> {
                        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                        if (bytes.length > maxRequestBytes) {
                            throw new DataBufferLimitException("Channel record exceeds limit");
                        }
                        return bytes;
                    })
                    .switchIfEmpty(Flux.error(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A channel requires a first record")));
            return stream(response, loadbalance.build(MediaType.APPLICATION_JSON, route.host())
                    .flatMapMany(rpc -> rpc.retrieveChannel(principalStash, route.path(), frames, responseType)));
        }
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                && !MediaType.APPLICATION_OCTET_STREAM.isCompatibleWith(contentType)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Use application/json or application/octet-stream"));
        }
        MediaType mimeType = MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_OCTET_STREAM;
        Mono<byte[]> body = DataBufferUtils.join(request.getBody(), maxRequestBytes)
                .map(RsocketRequestFactory::readAndRelease)
                .defaultIfEmpty(EMPTY_BODY);
        if (RSOCKET_MODEL_REQUEST_STREAM.equals(model) && format.ndjson()) {
            return stream(response, body.flatMapMany(bytes -> loadbalance.build(mimeType, route.host())
                    .flatMapMany(rpc -> rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, responseType))));
        }
        Mono<byte[]> result = body.flatMap(bytes -> loadbalance.build(mimeType, route.host()).flatMap(rpc -> {
            boolean jsonObjects = MediaType.APPLICATION_JSON.equals(mimeType) && RSOCKET_ECHO_OBJECT.equals(echo);
            if (RSOCKET_MODEL_REQUEST_STREAM.equals(model)) {
                Flux<byte[]> encoded = jsonObjects
                        ? rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, byte[].class)
                            .map(this::validateJson).filter(item -> item.length != 0)
                        : rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, responseType)
                            .map(RsocketRequestFactory::encode);
                return collectArray(encoded);
            }
            if (RSOCKET_ECHO_VOID.equals(echo)) {
                return rpc.retrieveMono(principalStash, mimeType, route.path(), bytes, Void.class)
                        .thenReturn(EMPTY_RESPONSE);
            }
            if (jsonObjects) {
                return rpc.retrieveMono(principalStash, mimeType, route.path(), bytes, byte[].class)
                        .map(value -> wrapJsonObject(validateJson(value))).defaultIfEmpty(EMPTY_RESPONSE);
            }
            return rpc.retrieveMono(principalStash, mimeType, route.path(), bytes, responseType)
                    .map(this::encodeResponse)
                    .defaultIfEmpty(EMPTY_RESPONSE);
        }));
        return result.timeout(requestTimeout, Mono.error(() -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "RSocket request timed out")))
                .flatMap(bytes -> {
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
                });
    }

    private static RequestFormat resolveFormat(Map<String, Object> metadata, HttpHeaders headers) {
        String model = option(metadata, "rsocketModel", headers.getFirst(RSOCKET_MODEL));
        String echo = option(metadata, "rsocketEcho", headers.getFirst(RSOCKET_ECHO));
        String configuredContentType = option(metadata, "rsocketContentType", null);
        String streamFormat = option(metadata, "rsocketStreamFormat", null);
        MediaType contentType;
        boolean ndjson;
        try {
            contentType = headers.getContentType();
            ndjson = (model == null || RSOCKET_MODEL_REQUEST_STREAM.equals(model))
                    && headers.getAccept().stream().anyMatch(type ->
                            MediaType.APPLICATION_NDJSON.includes(type) && type.getQualityValue() > 0);
        } catch (InvalidMediaTypeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid HTTP media type", error);
        }
        if (configuredContentType != null) {
            MediaType expected = MediaType.valueOf(configuredContentType);
            if (contentType != null && !expected.includes(contentType)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Content-Type does not match the RSocket route");
            }
            contentType = expected;
        }
        if (model == null) {
            if (MediaType.APPLICATION_NDJSON.includes(contentType)) {
                model = RSOCKET_MODEL_REQUEST_CHANNEL;
            } else {
                model = ndjson ? RSOCKET_MODEL_REQUEST_STREAM : RSOCKET_MODEL_REQUEST_RESPONSE;
            }
        }
        if (contentType == null) {
            contentType = RSOCKET_MODEL_REQUEST_CHANNEL.equals(model)
                    ? MediaType.APPLICATION_NDJSON : MediaType.APPLICATION_JSON;
        }
        if (streamFormat != null) {
            if (!"ndjson".equals(streamFormat) && !"json".equals(streamFormat)) {
                throw new IllegalArgumentException("rsocketStreamFormat must be json or ndjson");
            }
            ndjson = "ndjson".equals(streamFormat);
        }
        return new RequestFormat(model, echo == null ? RSOCKET_ECHO_OBJECT : echo, contentType, ndjson);
    }

    private static String option(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("Invalid route metadata: " + key);
    }

    private record RequestFormat(String model, String echo, MediaType contentType, boolean ndjson) {}

    private Mono<byte[]> collectArray(Flux<byte[]> values) {
        return values.collect(JsonArray::new, JsonArray::append).map(JsonArray::finish);
    }

    private final class JsonArray {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private int items;

        private JsonArray() {
            output.writeBytes(ARRAY_START);
        }

        private void append(byte[] value) {
            int separatorBytes = items == 0 ? 0 : 1;
            if (items >= maxResponseItems
                    || (long) output.size() + value.length + ARRAY_END.length + separatorBytes > maxResponseBytes) {
                throw responseLimit();
            }
            if (separatorBytes != 0) {
                output.write(',');
            }
            output.writeBytes(value);
            items++;
        }

        private byte[] finish() {
            output.writeBytes(ARRAY_END);
            return output.toByteArray();
        }
    }

    private byte[] validateJson(byte[] bytes) {
        if (bytes.length > maxResponseBytes) {
            throw responseLimit();
        }
        if (bytes.length == 0) {
            return EMPTY_BODY;
        }
        // A JSON value embedded in an envelope must be UTF-8 and cannot carry a BOM.
        int start = bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb
                && bytes[2] == (byte) 0xbf ? 3 : 0;
        try (var parser = UTF8_JSON.createParser(bytes, start, bytes.length - start)) {
            JsonToken root = parser.nextToken();
            if (root == null) {
                throw new IOException("Missing JSON value");
            }
            parser.skipChildren();
            if (parser.nextToken() != null) {
                throw new IOException("Expected one JSON value");
            }
            if (root == JsonToken.VALUE_NULL) {
                return EMPTY_BODY;
            }
            return start == 0 ? bytes : Arrays.copyOfRange(bytes, start, bytes.length);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "RSocket returned invalid UTF-8 JSON", error);
        }
    }

    private byte[] wrapJsonObject(byte[] value) {
        if (value.length == 0) {
            return EMPTY_RESPONSE;
        }
        long size = (long) OBJECT_START.length + value.length + 1;
        if (size > maxResponseBytes) {
            throw responseLimit();
        }
        byte[] bytes = Arrays.copyOf(OBJECT_START, (int) size);
        System.arraycopy(value, 0, bytes, OBJECT_START.length, value.length);
        bytes[bytes.length - 1] = '}';
        return bytes;
    }

    private Mono<Void> stream(ServerHttpResponse response, Flux<?> values) {
        response.getHeaders().setContentType(MediaType.APPLICATION_NDJSON);
        Flux<?> timed = values.timeout(Mono.delay(requestTimeout), value -> Mono.never(),
                Flux.error(() -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "RSocket first reply timed out")));
        return response.writeAndFlushWith(timed.map(value -> {
            byte[] encoded = encodeResponse(value);
            byte[] line = Arrays.copyOf(encoded, encoded.length + 1);
            line[encoded.length] = '\n';
            return Mono.fromSupplier(() -> response.bufferFactory().wrap(line));
        }));
    }

    private byte[] encodeResponse(Object value) {
        byte[] bytes = encode(new ResponseTemplete(value));
        if (bytes.length > maxResponseBytes) {
            throw responseLimit();
        }
        return bytes;
    }

    private static ResponseStatusException responseLimit() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "RSocket reply exceeds the configured response limit");
    }

    private static byte[] encode(Object value) {
        try {
            return JsonUtil.getJsonFactory().writeValueAsBytes(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not encode the RSocket reply", error);
        }
    }

    private static byte[] readAndRelease(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }
}
