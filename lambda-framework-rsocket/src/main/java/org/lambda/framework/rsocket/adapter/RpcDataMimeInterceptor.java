package org.lambda.framework.rsocket.adapter;

import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.exceptions.RejectedException;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import io.rsocket.util.RSocketProxy;

import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.RSocketExceptionEnum;
import org.reactivestreams.Publisher;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class RpcDataMimeInterceptor implements SocketAcceptorInterceptor {
    private static final MimeType FORY = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);
    private static final String ROUTING_MIME = WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString();
    private static final String COMPOSITE_MIME = WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString();

    @Override
    public SocketAcceptor apply(SocketAcceptor acceptor) {
        return (setup, sender) -> {
            MimeType mime;
            try {
                mime = MimeType.valueOf(setup.dataMimeType());
            } catch (InvalidMimeTypeException error) {
                return Mono.error(new EventException(RSocketExceptionEnum.SETUP_REJECTED, "Invalid RSocket data MIME type", error));
            }
            if (FORY.equalsTypeAndSubtype(mime)) {
                return acceptor.accept(setup, sender);
            }
            if (!MimeTypeUtils.APPLICATION_JSON.equalsTypeAndSubtype(mime)
                    && !MimeTypeUtils.APPLICATION_OCTET_STREAM.equalsTypeAndSubtype(mime)) {
                return Mono.error(new EventException(RSocketExceptionEnum.SETUP_REJECTED, "Unsupported RSocket data MIME type"));
            }
            if (!COMPOSITE_MIME.equals(setup.metadataMimeType())) {
                return Mono.error(new EventException(RSocketExceptionEnum.SETUP_REJECTED, "Gateway requests require RSocket composite metadata"));
            }
            return acceptor.accept(setup, sender).map(socket -> new RSocketProxy(socket) {
                @Override
                public Mono<Payload> requestResponse(Payload payload) {
                    return gatewayRoute(payload) ? super.requestResponse(payload) : rejectRequest(payload);
                }

                @Override
                public Flux<Payload> requestStream(Payload payload) {
                    return gatewayRoute(payload) ? super.requestStream(payload) : rejectRequest(payload).flux();
                }

                @Override
                public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                    return Flux.from(payloads).switchOnFirst((signal, input) -> {
                        if (!signal.hasValue()) {
                            return input;
                        }
                        if (gatewayRoute(signal.get())) {
                            return super.requestChannel(input);
                        }
                        return input.take(1, true).flatMap(RpcDataMimeInterceptor::rejectRequest)
                                .doOnDiscard(Payload.class, ReferenceCountUtil::safeRelease);
                    });
                }

                @Override
                public Mono<Void> fireAndForget(Payload payload) {
                    return reject(payload);
                }

                @Override
                public Mono<Void> metadataPush(Payload payload) {
                    return reject(payload);
                }
            });
        };
    }

    private static boolean gatewayRoute(Payload payload) {
        if (!payload.hasMetadata()) {
            return false;
        }
        String route = null;
        try {
            for (CompositeMetadata.Entry entry : new CompositeMetadata(payload.metadata(), false)) {
                if (ROUTING_MIME.equals(entry.getMimeType())) {
                    var routes = new RoutingMetadata(entry.getContent()).iterator();
                    if (route != null || !routes.hasNext()) {
                        return false;
                    }
                    route = routes.next();
                    if (routes.hasNext()) {
                        return false;
                    }
                }
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException malformedMetadata) {
            return false;
        }
        return route != null && (route.startsWith("/") || GatewayInteraction.DISCOVERY_ROUTE.equals(route));
    }

    private static <T> Mono<T> reject(Payload payload) {
        ReferenceCountUtil.safeRelease(payload);
        return Mono.error(rejection());
    }

    private static Mono<Payload> rejectRequest(Payload payload) {
        return reject(payload);
    }

    private static RejectedException rejection() {
        String message = "Java RSocket RPC requires application/x-lambda-fory";
        return new RejectedException(message, new EventException(RSocketExceptionEnum.REQUEST_REJECTED, message));
    }
}
