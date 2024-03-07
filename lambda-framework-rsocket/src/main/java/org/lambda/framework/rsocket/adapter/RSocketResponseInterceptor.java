/*
package org.lambda.framework.rsocket.adapter;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.lambda.framework.common.enums.SecurityContract.PRINCIPAL_STASH_NAMING;

public class RSocketResponseInterceptor implements RSocketInterceptor {

    @Override
    public RSocket apply(RSocket rSocket) {
        return new RSocket() {
            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                return rSocket.fireAndForget(payload);
            }

            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                return rSocket.requestResponse(payload);
            }

            @Override
            public Flux<Payload> requestStream(Payload payload) {
                String s = payload.getMetadataUtf8();
                String e = payload.getDataUtf8();
                return rSocket.requestStream(payload).contextWrite(Context.of(PRINCIPAL_STASH_NAMING,PRINCIPAL_STASH_NAMING))
                        .flatMap(xx->{
                            return Mono.deferContextual(Mono::just).map(x->{
                                return xx;
                            });
                        });
            }

            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                return rSocket.requestChannel(payloads);
            }

            @Override
            public Mono<Void> metadataPush(Payload payload) {
                return rSocket.metadataPush(payload);
            }

            @Override
            public double availability() {
                return rSocket.availability();
            }

            @Override
            public void dispose() {
                rSocket.dispose();
            }

            @Override
            public boolean isDisposed() {
                return rSocket.isDisposed();
            }

            @Override
            public Mono<Void> onClose() {
                return rSocket.onClose();
            }

            @Override
            public int hashCode() {
                return rSocket.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return rSocket.equals(obj);
            }

            @Override
            public String toString() {
                return rSocket.toString();
            }

        };
    }

}
*/
