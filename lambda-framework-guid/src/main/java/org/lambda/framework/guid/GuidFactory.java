package org.lambda.framework.guid;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.GuidExceptionEnum;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class GuidFactory {
    private static final long EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final int MAX_WORKER = 1023;
    private static final int MAX_SEQUENCE = 4095;
    private static final long MAX_TIMESTAMP = (1L << 41) - 1;
    private static final long MAX_SKEW_MILLIS = 5L;

    private final Clock clock;
    private final int worker;
    private final AtomicLong state = new AtomicLong(-1L);

    public GuidFactory(int worker) {
        this(worker, Clock.systemUTC());
    }

    GuidFactory(int worker, Clock clock) {
        if (worker < 0 || worker > MAX_WORKER) {
            throw new EventException(GuidExceptionEnum.CONFIGURATION_INVALID, "Snowflake worker must be 0..1023");
        }
        if (clock == null) {
            throw new EventException(GuidExceptionEnum.CONFIGURATION_INVALID, "Snowflake clock is required");
        }
        this.worker = worker;
        this.clock = clock;
    }

    public String nextId() {
        return Long.toString(nextLong());
    }

    public long nextLong() {
        long now = clock.millis() - EPOCH;
        if (now < 0 || now > MAX_TIMESTAMP) {
            throw new EventException(GuidExceptionEnum.CLOCK_OUT_OF_RANGE,
                    "Clock is outside the Snowflake epoch range");
        }
        while (true) {
            long prev = state.get();
            long prevMillis = prev < 0 ? -1L : (prev >>> 12);
            int prevSeq = prev < 0 ? 0 : (int) (prev & MAX_SEQUENCE);

            long nextMillis;
            int nextSeq;
            if (now > prevMillis) {
                nextMillis = now;
                nextSeq = 0;
            } else {
                long drift = prevMillis - now;
                if (drift > MAX_SKEW_MILLIS) {
                    throw new EventException(GuidExceptionEnum.CLOCK_MOVED_BACKWARDS,
                            "Clock moved backwards; Snowflake ID generation is suspended");
                }
                if (prevSeq < MAX_SEQUENCE) {
                    nextMillis = prevMillis;
                    nextSeq = prevSeq + 1;
                } else if (drift < MAX_SKEW_MILLIS) {
                    nextMillis = prevMillis + 1;
                    if (nextMillis > MAX_TIMESTAMP) {
                        throw new EventException(GuidExceptionEnum.CLOCK_OUT_OF_RANGE,
                                "Clock is outside the Snowflake epoch range");
                    }
                    nextSeq = 0;
                } else {
                    throw new EventException(GuidExceptionEnum.SEQUENCE_EXHAUSTED,
                            "Snowflake worker exhausted 4096 IDs in the current millisecond");
                }
            }
            long nextState = (nextMillis << 12) | nextSeq;
            if (state.compareAndSet(prev, nextState)) {
                return (nextMillis << 22) | ((long) worker << 12) | nextSeq;
            }
        }
    }
}
