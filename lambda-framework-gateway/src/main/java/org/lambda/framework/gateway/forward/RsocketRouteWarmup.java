package org.lambda.framework.gateway.forward;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

import static org.lambda.framework.gateway.enums.GatewayContract.RB_SCHEME;
import static org.lambda.framework.gateway.enums.GatewayContract.WS_SCHEME;

public final class RsocketRouteWarmup {
    private static final Logger log = LoggerFactory.getLogger(RsocketRouteWarmup.class);
    private static final Duration WARMUP_TIMEOUT = Duration.ofSeconds(5);
    private final ObjectProvider<RouteLocator> routes;
    private final RsocketRequestFactory requests;
    private final Disposable.Swap warmup = Disposables.swap();
    private final Disposable.Swap refresh = Disposables.swap();
    private long revision;

    public RsocketRouteWarmup(ObjectProvider<RouteLocator> routes, RsocketRequestFactory requests) {
        this.routes = routes;
        this.requests = requests;
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        routes.ifAvailable(locator -> start(locator));
    }

    private synchronized void start(RouteLocator locator) {
        if (warmup.isDisposed()) {
            return;
        }
        warmup.update(snapshot(locator, ++revision)
                .flatMapMany(Flux::fromIterable)
                .filter(RsocketRouteWarmup::enabled)
                .flatMap(route -> Mono.defer(() -> requests.warmRoute(route))
                        .timeout(WARMUP_TIMEOUT)
                        .onErrorResume(error -> {
                            log.debug("Could not warm RSocket route {}", route.getId(), error);
                            return Mono.empty();
                        }), 4)
                .subscribe(ignored -> {}, error -> log.warn("Could not read gateway routes for RSocket warmup", error)));
    }

    @EventListener
    synchronized void refresh(RefreshRoutesResultEvent event) {
        if (event.isSuccess() && event.getSource() instanceof RouteLocator locator && !refresh.isDisposed()) {
            refresh.update(snapshot(locator, ++revision)
                    .subscribe(ignored -> {}, error -> log.warn("Could not read refreshed routes for RSocket pools", error)));
        }
    }

    private Mono<List<Route>> snapshot(RouteLocator locator, long currentRevision) {
        return Flux.defer(locator::getRoutes).collectList()
                .doOnNext(routes -> retain(currentRevision, routes))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private synchronized void retain(long currentRevision, List<Route> routes) {
        if (currentRevision == revision) {
            requests.retainRoutePools(routes);
        }
    }

    private static boolean enabled(Route route) {
        var uri = route.getUri();
        return uri.getHost() != null
                && (RB_SCHEME.equals(uri.getScheme()) || WS_SCHEME.equals(uri.getScheme()))
                && Boolean.parseBoolean(String.valueOf(route.getMetadata().getOrDefault("rsocketWarmup", true)));
    }

    @PreDestroy
    synchronized void close() {
        revision++;
        refresh.dispose();
        warmup.dispose();
    }
}
