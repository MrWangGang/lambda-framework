package org.lambda.framework.common.support;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

/** Marks a streamed response that can consume a request publisher. */
public final class BiFlux<T> extends Flux<T> {
    private final Flux<T> delegate;

    private BiFlux(Flux<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> BiFlux<T> of(Publisher<? extends T> source) {
        return new BiFlux<>(Flux.from(source));
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> subscriber) {
        delegate.subscribe(subscriber);
    }
}
