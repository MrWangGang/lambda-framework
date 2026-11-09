package org.lambda.framework.common.support;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

/** A Flux whose RPC method represents a request-channel interaction. */
public final class TFlux<T> extends Flux<T> {
    private final Flux<T> delegate;

    private TFlux(Flux<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> TFlux<T> of(Publisher<? extends T> source) {
        return new TFlux<>(Flux.from(source));
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> subscriber) {
        delegate.subscribe(subscriber);
    }
}
