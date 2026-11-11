package org.lambda.framework.common.support;

import reactor.core.publisher.Flux;

import java.util.Set;

/** Emits an initial set of reachable endpoints and subsequent discovery snapshots for one service. */
@FunctionalInterface
public interface ServiceInstanceSource {
    record Endpoint(String host, int port, String routeVersion) {}

    Flux<Set<Endpoint>> watch(String service);
}
