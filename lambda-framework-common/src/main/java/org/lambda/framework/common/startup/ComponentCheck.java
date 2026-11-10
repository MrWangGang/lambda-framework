package org.lambda.framework.common.startup;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

public record ComponentCheck(String component, Supplier<Mono<Void>> verify) {
    Mono<Void> run(Duration timeout) {
        return Mono.defer(verify).timeout(timeout)
                .onErrorMap(error -> new IllegalStateException(component + " 启动检查失败", error));
    }
}
