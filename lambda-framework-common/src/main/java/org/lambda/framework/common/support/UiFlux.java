package org.lambda.framework.common.support;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

/** Marks a response that the HTTP gateway streams one way. */
public final class UiFlux<T> extends Flux<T> {
    private final Flux<T> delegate;

    private UiFlux(Flux<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> UiFlux<T> of(Publisher<? extends T> source) {
        return new UiFlux<>(Flux.from(source));
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> subscriber) {
        delegate.subscribe(subscriber);
    }
}
