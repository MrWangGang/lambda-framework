package org.lambda.framework.gateway.forward;

import jakarta.annotation.PreDestroy;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;


import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.common.util.OptionalUtil;
import org.lambda.framework.common.util.ReactiveValues;
import org.lambda.framework.enums.GatewayExceptionEnum;
import org.lambda.framework.enums.GatewayHttpExceptionEnum;
import org.lambda.framework.gateway.error.GatewayGlobalExceptionHandler;
import org.lambda.framework.gateway.filter.GatewayFilter;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance.Traffic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.lambda.framework.gateway.enums.GatewayContract.RSOCKET_ECHO;
import static org.lambda.framework.gateway.enums.GatewayContract.RSOCKET_ECHO_CHAR;
import static org.lambda.framework.gateway.enums.GatewayContract.RSOCKET_ECHO_VOID;

public class RsocketRequestFactory {
    private static final byte[] EMPTY_BODY = new byte[0];
    private static final byte[] EMPTY_RESPONSE = encode(new ResponseTemplete());
    private static final byte[] EMPTY_STREAM_LINE = line(encode(new ResponseTemplete(List.of())));
    private static final String DATA_START = new String(EMPTY_RESPONSE, StandardCharsets.UTF_8)
            .replaceFirst("}$", "") + ",\"data\":";
    private static final byte[] OBJECT_START = DATA_START.getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARRAY_START = (DATA_START + "[").getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARRAY_END = {']', '}'};
    private static final JsonFactory UTF8_JSON = JsonUtil.mapper().getFactory().copy()
            .disable(JsonFactory.Feature.CHARSET_DETECTION);

    private final RSocketLoadbalance loadbalance;
    private final RsocketStreamRoutes streamRoutes;
    private final PrincipalStash principalStash;
    private final ObjectProvider<WebSocketService> webSocketServices;
    private final int maxRequestBytes;
    private final int maxResponseBytes;
    private final int maxResponseItems;
    private final Duration requestTimeout;
    private final StringDecoder lineDecoder;
    private final Jackson2JsonDecoder channelJsonDecoder;

    public RsocketRequestFactory(RSocketLoadbalance loadbalance, PrincipalStash principalStash,
                                 ObjectProvider<WebSocketService> webSocketServices,
                                 @Value("${lambda.gateway.rsocket.max-request-bytes:1048576}") int maxRequestBytes,
                                 @Value("${lambda.gateway.rsocket.max-response-bytes:4194304}") int maxResponseBytes,
                                 @Value("${lambda.gateway.rsocket.max-response-items:512}") int maxResponseItems,
                                 @Value("${lambda.gateway.rsocket.request-timeout:15s}") Duration requestTimeout) {
        if (maxRequestBytes < 1 || maxResponseBytes < ARRAY_START.length + ARRAY_END.length
                || maxResponseItems < 1 || requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new EventException(GatewayExceptionEnum.UNEXPECTED_FAILURE, "Invalid HTTP to RSocket bridge limits");
        }
        this.loadbalance = loadbalance;
        this.streamRoutes = new RsocketStreamRoutes(loadbalance);
        this.principalStash = principalStash;
        this.webSocketServices = webSocketServices;
        this.maxRequestBytes = maxRequestBytes;
        this.maxResponseBytes = maxResponseBytes;
        this.maxResponseItems = maxResponseItems;
        this.requestTimeout = requestTimeout;
        this.lineDecoder = StringDecoder.allMimeTypes(List.of("\n"), true);
        this.lineDecoder.setMaxInMemorySize(maxRequestBytes);
        this.lineDecoder.setDefaultCharset(StandardCharsets.UTF_8);
        this.channelJsonDecoder = new Jackson2JsonDecoder(JsonUtil.mapper());
        this.channelJsonDecoder.setMaxInMemorySize(maxRequestBytes);
    }

    public Mono<Void> execute(ServerWebExchange exchange, GatewayFilterChain chain,
                              GatewayFilter.GlobalFilterRoute route) {
        return Mono.defer(() -> forward(exchange, route));
    }

    public Mono<Void> executeWebSocket(ServerWebExchange exchange, GatewayFilter.GlobalFilterRoute route) {
        if (!"websocket".equalsIgnoreCase(exchange.getRequest().getHeaders().getUpgrade())) {
            return Mono.error(new EventException(GatewayHttpExceptionEnum.UPGRADE_REQUIRED, "WebSocket upgrade required"));
        }
        return interaction(route).flatMap(contract -> {
            if (contract.interaction() != GatewayInteraction.REQUEST_CHANNEL) {
                return Mono.error(new EventException(GatewayHttpExceptionEnum.NOT_FOUND, "No public channel for this path"));
            }
            var webSocketService = OptionalUtil.require(webSocketServices.getIfAvailable(),
                    () -> new EventException(GatewayHttpExceptionEnum.SERVICE_UNAVAILABLE, "WebSocket service unavailable"));
            return Mono.defer(principalStash::setSecurityStash)
                    .switchIfEmpty(Mono.fromSupplier(SecurityStash::new))
                    .flatMap(stash -> webSocketService.handleRequest(exchange,
                            session -> webSocketChannel(exchange.getResponse(), session, route, () -> Mono.just(stash))));
        });
    }

    private Mono<Void> webSocketChannel(ServerHttpResponse response, WebSocketSession session,
                                        GatewayFilter.GlobalFilterRoute route, PrincipalStash sessionPrincipal) {
        // Keep the socket reader attached until the error reply is flushed, even if the
        // RSocket requester cancels its input subscription before reporting the error.
        Flux<byte[]> frames = session.receive().map(message -> {
                    try {
                        return Signal.next(readWebSocketFrame(message));
                    } catch (RuntimeException error) {
                        return Signal.<byte[]>error(error);
                    }
                })
                .publish(1).autoConnect(1).dematerialize();
        Flux<byte[]> replies = frames.switchOnFirst((first, input) -> {
                    if (!first.hasValue()) {
                        return input;
                    }
                    return timeoutFirstReply(interaction(route).flatMapMany(contract -> {
                        if (contract.interaction() != GatewayInteraction.REQUEST_CHANNEL) {
                            return Flux.error(new EventException(GatewayHttpExceptionEnum.NOT_FOUND, "No public channel for this path"));
                        }
                        return channelReplies(route, contract, input, sessionPrincipal);
                    })).map(value -> encodeResponse(List.of(value)));
                })
                .onErrorResume(error -> Mono.fromSupplier(() ->
                        encode(GatewayGlobalExceptionHandler.errorResponse(response, error, true))));
        return session.send(replies.takeUntilOther(session.closeStatus()).map(bytes ->
                        new WebSocketMessage(WebSocketMessage.Type.TEXT, session.bufferFactory().wrap(bytes))))
                .then(Mono.defer(session::close));
    }

    private byte[] readWebSocketFrame(WebSocketMessage message) {
        if (message.getType() != WebSocketMessage.Type.TEXT) {
            throw new EventException(GatewayHttpExceptionEnum.UNSUPPORTED_MEDIA_TYPE, "Send one JSON value per text message");
        }
        var payload = message.getPayload();
        int size = payload.readableByteCount();
        if (size > maxRequestBytes) {
            throw new EventException(GatewayHttpExceptionEnum.CONTENT_TOO_LARGE, "WebSocket message exceeds limit");
        }
        byte[] bytes = new byte[size];
        payload.read(bytes);
        try {
            byte[] json = parseJsonValue(bytes, GatewayHttpExceptionEnum.BAD_REQUEST);
            if (json.length != 0) {
                return json;
            }
        } catch (IOException error) {
            throw new EventException(GatewayHttpExceptionEnum.BAD_REQUEST, "Invalid WebSocket JSON message", error);
        }
        throw new EventException(GatewayHttpExceptionEnum.BAD_REQUEST, "WebSocket JSON message must contain a value");
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilter.GlobalFilterRoute route) {
        var request = exchange.getRequest();
        if (request.getMethod() != HttpMethod.POST) {
            return Mono.error(new EventException(GatewayHttpExceptionEnum.METHOD_NOT_ALLOWED));
        }
        if (!request.getQueryParams().isEmpty()) {
            return Mono.error(new EventException(GatewayHttpExceptionEnum.BAD_REQUEST, "Use a request DTO in the POST body"));
        }
        HttpHeaders headers = request.getHeaders();
        Map<String, Object> metadata = route.metadata();
        return interaction(route).flatMap(contract -> {
            exchange.getAttributes().put(GatewayGlobalExceptionHandler.INTERACTION_ATTRIBUTE, contract.interaction());
            MediaType contentType = resolveContentType(metadata, headers);
            return forward(exchange, route, contract, contentType);
        });
    }

    private Mono<RsocketStreamRoutes.Route> interaction(GatewayFilter.GlobalFilterRoute route) {
        String path = route.path();
        if (!GatewayInteraction.isPublicPath(path)) {
            return Mono.error(new EventException(GatewayHttpExceptionEnum.NOT_FOUND, "No public MessageMapping for this path"));
        }
        return streamRoutes.resolve(route.host(), path);
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilter.GlobalFilterRoute route,
                               RsocketStreamRoutes.Route contract, MediaType contentType) {
        var interaction = contract.interaction();
        var request = exchange.getRequest();
        var response = exchange.getResponse();
        if (interaction == GatewayInteraction.REQUEST_CHANNEL) {
            boolean ndjson = MediaType.APPLICATION_NDJSON.isCompatibleWith(contentType);
            if (!ndjson && !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                return Mono.error(new EventException(GatewayHttpExceptionEnum.UNSUPPORTED_MEDIA_TYPE,
                        "request/channel requires JSON or NDJSON input"));
            }
            Flux<byte[]> decodedFrames = ndjson
                    ? lineDecoder.decode(request.getBody(), ResolvableType.forClass(String.class),
                            MediaType.APPLICATION_NDJSON, Map.of())
                        .filter(line -> !line.isBlank()).map(line -> line.getBytes(StandardCharsets.UTF_8))
                    : channelJsonDecoder.decode(request.getBody(), ResolvableType.forClass(JsonNode.class),
                            contentType, Map.of()).map(RsocketRequestFactory::encode);
            Flux<byte[]> frames = decodedFrames.map(bytes -> {
                        if (bytes.length > maxRequestBytes) {
                            throw new EventException(GatewayHttpExceptionEnum.CONTENT_TOO_LARGE, "Channel record exceeds limit");
                        }
                        return bytes;
                    })
                    .switchIfEmpty(Flux.error(() -> new EventException(GatewayHttpExceptionEnum.BAD_REQUEST,
                            "A channel requires a first record")));
            return stream(response, channelReplies(route, contract, frames, principalStash));
        }
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                && !MediaType.APPLICATION_OCTET_STREAM.isCompatibleWith(contentType)) {
            return Mono.error(new EventException(GatewayHttpExceptionEnum.UNSUPPORTED_MEDIA_TYPE,
                    "Use application/json or application/octet-stream"));
        }
        MediaType mimeType = MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_OCTET_STREAM;
        Mono<DataBuffer> requestBody = DataBufferUtils.join(request.getBody(), maxRequestBytes);
        boolean json = MediaType.APPLICATION_JSON.equals(mimeType);
        if (json && interaction == GatewayInteraction.REQUEST_RESPONSE) {
            return forwardJsonRequest(response, requestBody, mimeType, route, contract);
        }
        Mono<byte[]> body = requestBody
                .map(RsocketRequestFactory::readAndRelease)
                .defaultIfEmpty(EMPTY_BODY);
        boolean requestStream = !interaction.isUnary();
        String binaryEcho = json ? "" : option(route.metadata(), "rsocketEcho")
                .or(() -> Optional.ofNullable(request.getHeaders().getFirst(RSOCKET_ECHO))).orElse("");
        Class<?> responseType = RSOCKET_ECHO_CHAR.equals(binaryEcho) ? String.class : Object.class;
        if (interaction.isNdjson()) {
            return stream(response, body.flatMapMany(bytes -> requester(mimeType, route, contract, Traffic.STREAM)
                    .flatMapMany(rpc -> rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, responseType))));
        }
        Mono<byte[]> result = body.flatMap(bytes -> requester(mimeType, route, contract, requestStream ? Traffic.STREAM : Traffic.UNARY).flatMap(rpc -> {
            if (requestStream) {
                Flux<byte[]> encoded = json
                        ? rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, byte[].class)
                            .map(this::validateJson).filter(item -> item.length != 0)
                        : rpc.retrieveFlux(principalStash, mimeType, route.path(), bytes, responseType)
                            .map(RsocketRequestFactory::encode);
                return collectArray(encoded);
            }
            if (interaction == GatewayInteraction.REQUEST_VOID || !json && RSOCKET_ECHO_VOID.equals(binaryEcho)) {
                return rpc.retrieveMono(principalStash, mimeType, route.path(), bytes, Void.class).thenReturn(EMPTY_RESPONSE);
            }
            return requireReply(rpc.retrieveMono(principalStash, mimeType, route.path(), bytes, responseType))
                    .map(this::encodeResponse);
        }));
        return writeJsonResponse(response, result);
    }

    private Mono<Void> forwardJsonRequest(ServerHttpResponse response, Mono<DataBuffer> requestBody, MediaType mimeType,
                                          GatewayFilter.GlobalFilterRoute route, RsocketStreamRoutes.Route contract) {
        var buffered = requestBody.map(BufferedRequest::new)
                .switchIfEmpty(Mono.fromSupplier(() -> new BufferedRequest(null)));
        var result = Mono.usingWhen(buffered, body -> requester(mimeType, route, contract, Traffic.UNARY)
                        .flatMap(rpc -> rpc.retrieveMono(principalStash, mimeType, route.path(),
                                        Mono.fromSupplier(body::transfer), DataBuffer.class)
                                .mapNotNull(this::jsonEnvelope)),
                body -> Mono.fromRunnable(body::release))
                .doOnDiscard(BufferedRequest.class, BufferedRequest::release);
        return writeJsonResponse(response, requireReply(result));
    }

    private static final class BufferedRequest {
        private final AtomicReference<DataBuffer> buffer;

        private BufferedRequest(DataBuffer buffer) {
            this.buffer = new AtomicReference<>(buffer);
        }

        private DataBuffer transfer() {
            // RSocket owns the buffer once its request publisher takes it.
            return buffer.getAndSet(null);
        }

        private void release() {
            DataBufferUtils.release(buffer.getAndSet(null));
        }
    }

    private Mono<Void> writeJsonResponse(ServerHttpResponse response, Mono<byte[]> result) {
        return result.timeout(requestTimeout, Mono.error(() -> new EventException(GatewayHttpExceptionEnum.GATEWAY_TIMEOUT,
                        "RSocket request timed out"))).flatMap(bytes -> {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
        });
    }

    private Flux<Object> channelReplies(GatewayFilter.GlobalFilterRoute route, RsocketStreamRoutes.Route contract,
                                        Flux<byte[]> frames, PrincipalStash principal) {
        return requester(MediaType.APPLICATION_JSON, route, contract, Traffic.CHANNEL)
                .flatMapMany(rpc -> rpc.retrieveChannel(principal, route.path(), frames, Object.class));
    }

    private Mono<RSocketLoadbalance.ProcessResponse> requester(MediaType mimeType,
            GatewayFilter.GlobalFilterRoute route, RsocketStreamRoutes.Route contract, Traffic traffic) {
        Map<String, Object> metadata = route.metadata();
        String specific = switch (traffic) {
            case UNARY -> "rsocketConnectionsPerInstance";
            case STREAM -> "rsocketStreamConnectionsPerInstance";
            case CHANNEL -> "rsocketChannelConnectionsPerInstance";
        };
        String key = metadata.containsKey(specific) ? specific : "rsocketConnectionsPerInstance";
        Object configured = metadata.get(key);
        if (configured == null) {
            return loadbalance.buildForGateway(mimeType, route.host(), traffic, Optional.empty(), contract.endpoints());
        }
        int connections;
        try {
            connections = Integer.parseInt(configured.toString());
        } catch (NumberFormatException error) {
            throw new EventException(GatewayExceptionEnum.UNEXPECTED_FAILURE, key + " must be an integer", error);
        }
        return loadbalance.buildForGateway(mimeType, route.host(), traffic, Optional.of(connections), contract.endpoints());
    }

    @PreDestroy
    void close() {
        streamRoutes.close();
    }

    private static <T> Mono<T> requireReply(Mono<T> response) {
        return ReactiveValues.require(response,
                () -> new EventException(GatewayHttpExceptionEnum.BAD_GATEWAY, "Required RSocket response is empty"));
    }

    private static MediaType resolveContentType(Map<String, Object> metadata, HttpHeaders headers) {
        Optional<MediaType> contentType;
        try {
            contentType = Optional.ofNullable(headers.getContentType());
        } catch (InvalidMediaTypeException error) {
            throw new EventException(GatewayHttpExceptionEnum.BAD_REQUEST, "Invalid HTTP media type", error);
        }
        return option(metadata, "rsocketContentType").map(value -> {
            MediaType expected = MediaType.valueOf(value);
            if (contentType.filter(actual -> !expected.includes(actual)).isPresent()) {
                throw new EventException(GatewayHttpExceptionEnum.UNSUPPORTED_MEDIA_TYPE,
                        "Content-Type does not match the RSocket route");
            }
            return expected;
        }).orElseGet(() -> contentType.orElse(MediaType.APPLICATION_JSON));
    }

    private static Optional<String> option(Map<String, Object> metadata, String key) {
        return OptionalUtil.find(metadata, key).map(value -> {
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
            throw new EventException(GatewayExceptionEnum.UNEXPECTED_FAILURE, "Invalid route metadata: " + key);
        });
    }

    private Mono<byte[]> collectArray(Flux<byte[]> values) {
        return values.collect(JsonArray::new, JsonArray::append).map(JsonArray::finish);
    }

    private final class JsonArray {
        private final List<byte[]> items = new ArrayList<>();
        private int length = ARRAY_START.length + ARRAY_END.length;

        private void append(byte[] value) {
            long nextLength = (long) length + value.length + (items.isEmpty() ? 0 : 1);
            if (items.size() >= maxResponseItems || nextLength > maxResponseBytes) {
                throw responseLimit();
            }
            items.add(value);
            length = (int) nextLength;
        }

        private byte[] finish() {
            byte[] result = Arrays.copyOf(ARRAY_START, length);
            int offset = ARRAY_START.length;
            for (int index = 0; index < items.size(); index++) {
                if (index != 0) {
                    result[offset++] = ',';
                }
                byte[] item = items.get(index);
                System.arraycopy(item, 0, result, offset, item.length);
                offset += item.length;
            }
            System.arraycopy(ARRAY_END, 0, result, offset, ARRAY_END.length);
            return result;
        }
    }

    private byte[] validateJson(byte[] bytes) {
        if (bytes.length > maxResponseBytes) {
            throw responseLimit();
        }
        if (bytes.length == 0) {
            return EMPTY_BODY;
        }
        try {
            return parseJsonValue(bytes, GatewayHttpExceptionEnum.BAD_GATEWAY);
        } catch (IOException error) {
            throw new EventException(GatewayHttpExceptionEnum.BAD_GATEWAY, "RSocket returned invalid UTF-8 JSON", error);
        }
    }

    private static byte[] parseJsonValue(byte[] bytes, GatewayHttpExceptionEnum invalidJson) throws IOException {
        // A JSON value embedded in an envelope must be UTF-8 and cannot carry a BOM.
        int start = bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb
                && bytes[2] == (byte) 0xbf ? 3 : 0;
        try (var parser = UTF8_JSON.createParser(bytes, start, bytes.length - start)) {
            if (readJsonRoot(parser, invalidJson) == JsonToken.VALUE_NULL) {
                return EMPTY_BODY;
            }
            return start == 0 ? bytes : Arrays.copyOfRange(bytes, start, bytes.length);
        }
    }

    private static JsonToken readJsonRoot(JsonParser parser, GatewayHttpExceptionEnum invalidJson) throws IOException {
        JsonToken root = parser.nextToken();
        if (root == null) {
            throw new EventException(invalidJson, "Missing JSON value");
        }
        parser.skipChildren();
        if (parser.nextToken() != null) {
            throw new EventException(invalidJson, "Expected one JSON value");
        }
        return root;
    }

    private byte[] jsonEnvelope(DataBuffer value) {
        try {
            int size = value.readableByteCount();
            if (size == 0) {
                return null;
            }
            if (size > maxResponseBytes) {
                throw responseLimit();
            }

            int start = value.readPosition();
            if (size >= 3 && value.getByte(start) == (byte) 0xef && value.getByte(start + 1) == (byte) 0xbb
                    && value.getByte(start + 2) == (byte) 0xbf) {
                value.readPosition(start + 3);
                size -= 3;
            }

            // Copy directly from RSocket into the final HTTP body and validate just its JSON value.
            long total = (long) OBJECT_START.length + size + 1;
            if (total > Integer.MAX_VALUE) {
                throw responseLimit();
            }
            byte[] bytes = Arrays.copyOf(OBJECT_START, (int) total);
            value.read(bytes, OBJECT_START.length, size);
            try (var parser = UTF8_JSON.createParser(bytes, OBJECT_START.length, size)) {
                if (readJsonRoot(parser, GatewayHttpExceptionEnum.BAD_GATEWAY) == JsonToken.VALUE_NULL) {
                    return null;
                }
            }
            if (total > maxResponseBytes) {
                throw responseLimit();
            }
            bytes[bytes.length - 1] = '}';
            return bytes;
        } catch (IOException error) {
            throw new EventException(GatewayHttpExceptionEnum.BAD_GATEWAY, "RSocket returned invalid UTF-8 JSON", error);
        } finally {
            DataBufferUtils.release(value);
        }
    }

    private Mono<Void> stream(ServerHttpResponse response, Flux<?> values) {
        response.getHeaders().setContentType(MediaType.APPLICATION_NDJSON);
        response.getHeaders().set("X-Accel-Buffering", "no");
        var lines = timeoutFirstReply(values).map(value -> line(encodeResponse(List.of(value))))
                .defaultIfEmpty(EMPTY_STREAM_LINE)
                .onErrorResume(error -> Mono.fromSupplier(() ->
                        line(encode(GatewayGlobalExceptionHandler.errorResponse(response, error, true)))));
        return response.writeAndFlushWith(lines.map(bytes -> Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes))));
    }

    private <T> Flux<T> timeoutFirstReply(Flux<T> values) {
        return values.timeout(Mono.delay(requestTimeout), value -> Mono.never(),
                Flux.error(() -> new EventException(GatewayHttpExceptionEnum.GATEWAY_TIMEOUT, "RSocket first reply timed out")));
    }

    private static byte[] line(byte[] json) {
        byte[] line = Arrays.copyOf(json, json.length + 1);
        line[json.length] = '\n';
        return line;
    }

    private byte[] encodeResponse(Object value) {
        byte[] bytes = encode(new ResponseTemplete(value));
        if (bytes.length > maxResponseBytes) {
            throw responseLimit();
        }
        return bytes;
    }

    private static EventException responseLimit() {
        return new EventException(GatewayHttpExceptionEnum.BAD_GATEWAY, "RSocket reply exceeds the configured response limit");
    }

    private static byte[] encode(Object value) {
        try {
            return JsonUtil.mapper().writeValueAsBytes(value);
        } catch (JsonProcessingException error) {
            throw new EventException(GatewayExceptionEnum.UNEXPECTED_FAILURE, "Could not encode the RSocket reply", error);
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
