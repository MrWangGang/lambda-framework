package org.lambda.framework.rsocket.adapter;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.plugins.RSocketInterceptor;
import org.lambda.framework.common.support.SecurityStash;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;

@Component
public class RSocketMetadataInterceptor implements RSocketInterceptor {
    @Override
    public RSocket apply(RSocket rSocket) {
        return createRSocket(rSocket);
    }
    private SecurityStash getValueFromMetadata(ByteBuf metadata) {
        SecurityStash securityStash = SecurityStash.builder().build();
        CompositeMetadata compositeMetadata = new CompositeMetadata(metadata, false);
        if(compositeMetadata!=null){
            compositeMetadata.stream()
                    .forEach(entry -> {
                        if(AUTHTOKEN_STASH_NAMING.equals(entry.getMimeType())){
                            ByteBuf content = entry.getContent();
                            String _authtoken = content.toString(io.netty.util.CharsetUtil.UTF_8);
                            securityStash.setAuthToken(_authtoken);
                        }
                        if(PRINCIPAL_STASH_NAMING.equals(entry.getMimeType())){
                            ByteBuf content = entry.getContent();
                            String _principal = content.toString(io.netty.util.CharsetUtil.UTF_8);
                            securityStash.setPrincipal(_principal);
                        }
                    });
            return securityStash;
        }
        return securityStash;
    }
    private RSocket createRSocket(RSocket sendingSocket) {
        return new RSocket() {
            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                SecurityStash securityStash = getValueFromMetadata(payload.metadata());
                return sendingSocket.fireAndForget(payload)
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }

            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                SecurityStash securityStash = getValueFromMetadata(payload.metadata());
                return sendingSocket.requestResponse(payload)
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }

            @Override
            public Flux<Payload> requestStream(Payload payload) {
                SecurityStash securityStash = getValueFromMetadata(payload.metadata());
                return sendingSocket.requestStream(payload)
                        .contextWrite(Context.of(SecurityStash.class,securityStash));
            }

            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
               /* return Flux.from(payloads)
                        .map(payload -> {
                            SecurityStash securityStash = getValueFromMetadata(payload.metadata());
                            return Tuples.of(securityStash, payload);
                        })
                        .flatMap(tuple -> {
                            SecurityStash securityStash = tuple.getT1();
                            Payload originalPayload = tuple.getT2();
                            return sendingSocket.requestChannel(Mono.just(originalPayload))
                                    .contextWrite(SecurityStash.class,securityStash)
                        });*/
                return sendingSocket.requestChannel(payloads);
            }

            @Override
            public Mono<Void> metadataPush(Payload payload) {
                SecurityStash securityStash = getValueFromMetadata(payload.metadata());
                return sendingSocket.metadataPush(payload)
                        .contextWrite(Context.of(AUTHTOKEN_STASH_NAMING,securityStash.getAuthToken()))
                        .contextWrite(Context.of(PRINCIPAL_STASH_NAMING,securityStash.getPrincipal()));

            }

            @Override
            public double availability() {
                return sendingSocket.availability();
            }

            @Override
            public void dispose() {
                sendingSocket.dispose();
            }

            @Override
            public boolean isDisposed() {
                return sendingSocket.isDisposed();
            }

            @Override
            public Mono<Void> onClose() {
                return sendingSocket.onClose();
            }
        };
    }
}
