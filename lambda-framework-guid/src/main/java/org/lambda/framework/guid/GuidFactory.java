package org.lambda.framework.guid;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class GuidFactory {
    private static final long EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final int MAX_WORKER = 1023;
    private static final int MAX_SEQUENCE = 4095;
    private static final long MAX_TIMESTAMP = (1L << 41) - 1;

    private final Clock clock;
    private final int worker;
    private long lastMillis = -1;
    private int sequence;

    public GuidFactory(int worker) {
        this(worker, Clock.systemUTC());
    }

    GuidFactory(int worker, Clock clock) {
        if (worker < 0 || worker > MAX_WORKER) {
            throw new IllegalArgumentException("Snowflake worker must be 0..1023");
        }
        this.worker = worker;
        this.clock = Objects.requireNonNull(clock);
    }

    public String nextId() {
        return Long.toString(nextLong());
    }

    public synchronized long nextLong() {
        long now = clock.millis() - EPOCH;
        if (now < 0 || now > MAX_TIMESTAMP) {
            throw new IllegalStateException("Clock is outside the Snowflake epoch range");
        }
        if (now < lastMillis) {
            throw new IllegalStateException("Clock moved backwards; Snowflake ID generation is suspended");
        }
        int nextSequence = now == lastMillis ? sequence + 1 : 0;
        if (nextSequence > MAX_SEQUENCE) {
            throw new IllegalStateException("Snowflake worker exhausted 4096 IDs in the current millisecond");
        }
        lastMillis = now;
        sequence = nextSequence;
        return (now << 22) | ((long) worker << 12) | sequence;
    }
}
