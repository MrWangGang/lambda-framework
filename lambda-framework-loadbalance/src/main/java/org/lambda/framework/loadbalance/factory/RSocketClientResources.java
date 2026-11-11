package org.lambda.framework.loadbalance.factory;

import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

import java.time.Duration;

public final class RSocketClientResources implements AutoCloseable {
    private final LoopResources unary;
    private final LoopResources stream;
    private final LoopResources channel;

    public RSocketClientResources(int unaryWorkers, int streamWorkers, int channelWorkers) {
        requireValid(unaryWorkers);
        requireValid(streamWorkers);
        requireValid(channelWorkers);
        unary = LoopResources.create("lambda-rsocket-unary", unaryWorkers, true);
        stream = LoopResources.create("lambda-rsocket-stream", streamWorkers, true);
        channel = LoopResources.create("lambda-rsocket-channel", channelWorkers, true);
    }

    LoopResources forTraffic(RSocketLoadbalance.Traffic traffic) {
        return switch (traffic) {
            case UNARY -> unary;
            case STREAM -> stream;
            case CHANNEL -> channel;
        };
    }

    private static void requireValid(int workers) {
        if (workers < 1 || workers > 64) {
            throw new IllegalArgumentException("RSocket I/O workers per traffic class must be between 1 and 64");
        }
    }

    @Override
    public void close() {
        var timeout = Duration.ofSeconds(5);
        Mono.whenDelayError(unary.disposeLater(Duration.ZERO, timeout),
                stream.disposeLater(Duration.ZERO, timeout), channel.disposeLater(Duration.ZERO, timeout))
                .block(timeout.plusSeconds(1));
    }
}
