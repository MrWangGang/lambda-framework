package org.lambda.framework.rsocket.adapter;

import org.lambda.framework.rsocket.error.RsocketGlobalExceptionHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import io.rsocket.util.RSocketProxy;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.util.JsonUtil;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_003;

public class RSocketAcceptorInterceptor implements SocketAcceptorInterceptor {
    private static final MimeType FORY = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);
    private static final ResolvableType IDENTITY_TYPE = ResolvableType.forClass(SecurityStash.class);
    private static final int MAX_IDENTITY_BYTES = 48 * 1024;
    private static final com.fasterxml.jackson.databind.ObjectReader JSON_IDENTITY = JsonUtil.mapper()
            .readerFor(SecurityStash.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final SecurityStash ANONYMOUS = new SecurityStash();

    private final RsocketGlobalExceptionHandler errors;
    private final RSocketStrategies strategies;

    public RSocketAcceptorInterceptor(RsocketGlobalExceptionHandler errors, RSocketStrategies strategies) {
        this.errors = errors;
        this.strategies = strategies;
    }

    @Override
    public SocketAcceptor apply(SocketAcceptor acceptor) {
        return (setup, sender) -> acceptor.accept(setup, sender)
                .map(socket -> wrap(socket, RSocketContract.FORY_MIME_TYPE.equals(setup.dataMimeType())));
    }

    private SecurityStash readIdentity(Payload payload, boolean rpc) {
        if (!payload.hasMetadata()) {
            return ANONYMOUS;
        }
        SecurityStash stash = null;
        try {
            for (var entry : new CompositeMetadata(payload.metadata(), false)) {
                boolean binary = RSocketContract.FORY_MIME_TYPE.equals(entry.getMimeType());
                if (!binary && !MimeTypeUtils.APPLICATION_JSON_VALUE.equals(entry.getMimeType())) {
                    continue;
                }
                var content = entry.getContent();
                if (stash != null || binary != rpc || content.readableBytes() > MAX_IDENTITY_BYTES) {
                    throw new EventException(ES_RSOCKET_003);
                }
                if (binary) {
                    var buffer = DefaultDataBufferFactory.sharedInstance.wrap(content.nioBuffer());
                    stash = strategies.<SecurityStash>decoder(IDENTITY_TYPE, FORY)
                            .decode(buffer, IDENTITY_TYPE, FORY, Map.of());
                } else {
                    byte[] bytes = new byte[content.readableBytes()];
                    content.getBytes(content.readerIndex(), bytes);
                    stash = JSON_IDENTITY.readValue(bytes);
                }
                if (stash == null) {
                    throw new EventException(ES_RSOCKET_003);
                }
            }
            return stash == null ? ANONYMOUS : stash;
        } catch (RuntimeException | IOException error) {
            throw new EventException(ES_RSOCKET_003);
        }
    }

    private <T> Mono<T> unary(Payload payload, boolean rpc, Function<Payload, Mono<T>> delegate) {
        SecurityStash stash;
        try {
            stash = readIdentity(payload, rpc);
        } catch (RuntimeException error) {
            ReferenceCountUtil.safeRelease(payload);
            return Mono.error(errors.handleException(error));
        }
        return delegate.apply(payload).contextWrite(context -> stash == ANONYMOUS ? context.delete(SecurityStash.class) : context.put(SecurityStash.class, stash))
                .onErrorMap(errors::handleException);
    }

    private RSocket wrap(RSocket socket, boolean rpc) {
        return new RSocketProxy(socket) {
            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                return unary(payload, rpc, socket::fireAndForget);
            }

            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                return unary(payload, rpc, socket::requestResponse);
            }

            @Override
            public Flux<Payload> requestStream(Payload payload) {
                SecurityStash stash;
                try {
                    stash = readIdentity(payload, rpc);
                } catch (RuntimeException error) {
                    ReferenceCountUtil.safeRelease(payload);
                    return Flux.error(errors.handleException(error));
                }
                return socket.requestStream(payload).contextWrite(context -> stash == ANONYMOUS ? context.delete(SecurityStash.class) : context.put(SecurityStash.class, stash))
                        .onErrorMap(errors::handleException);
            }

            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return Flux.from(payloads).<Payload>switchOnFirst((signal, input) -> {
                    if (!signal.hasValue()) {
                        return input;
                    }
                    SecurityStash stash;
                    try {
                        stash = readIdentity(signal.get(), rpc);
                    } catch (RuntimeException error) {
                        return input.take(1, true).flatMap(payload -> {
                            ReferenceCountUtil.safeRelease(payload);
                            return Mono.<Payload>error(error);
                        }).doOnDiscard(Payload.class, ReferenceCountUtil::safeRelease);
                    }
                    return socket.requestChannel(input).contextWrite(context -> stash == ANONYMOUS ? context.delete(SecurityStash.class) : context.put(SecurityStash.class, stash));
                }).onErrorMap(errors::handleException);
            }

            @Override
            public Mono<Void> metadataPush(Payload payload) {
                return unary(payload, rpc, socket::metadataPush);
            }
        };
    }
}
