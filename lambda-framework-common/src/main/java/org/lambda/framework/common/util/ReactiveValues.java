package org.lambda.framework.common.util;

import reactor.core.publisher.Mono;

import org.lambda.framework.common.exception.RequiredValueException;

import java.util.Optional;
import java.util.function.Supplier;

public final class ReactiveValues {
    private ReactiveValues() {}

    public static <T> Mono<Optional<T>> optional(Mono<T> source) {
        return OptionalUtil.require(source).singleOptional();
    }

    public static <T> Mono<T> require(Mono<T> source) {
        return require(source, RequiredValueException::new);
    }

    public static <T> Mono<T> require(Mono<T> source, Supplier<? extends RuntimeException> missing) {
        return optional(source).map(result -> result.orElseThrow(missing));
    }
}
