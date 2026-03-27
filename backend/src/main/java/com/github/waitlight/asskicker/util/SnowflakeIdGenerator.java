package com.github.waitlight.asskicker.util;

/**
 * Snowflake-style distributed unique ID generator.
 * <p>
 * Bit layout: 41 bits timestamp, 5 bits datacenter id, 5 bits worker id, 12
 * bits sequence.
 * IDs are time-ordered and fit in a signed {@code long} when epoch and current
 * time are chosen consistently.
 */
public final class SnowflakeIdGenerator {

    /** Custom epoch (ms); IDs are relative to this instant. */
    public static final long DEFAULT_EPOCH_MS = 1577836800000L; // 2020-01-01T00:00:00Z

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * Max tolerated clock drift (ms) before {@link #nextId()} throws. Smaller
     * drifts spin until caught up.
     */
    private static final long MAX_BACKWARD_OFFSET_MS = 5L;

    private final long epochMs;
    private final long workerId;
    private final long datacenterId;

    private long sequence;
    private long lastTimestamp;

    /**
     * @param workerId     0 .. 31
     * @param datacenterId 0 .. 31
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this(DEFAULT_EPOCH_MS, workerId, datacenterId);
    }

    /**
     * @param epochMs      custom epoch in milliseconds
     * @param workerId     0 .. 31
     * @param datacenterId 0 .. 31
     */
    public SnowflakeIdGenerator(long epochMs, long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    "workerId must be between 0 and " + MAX_WORKER_ID + " but was " + workerId);
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    "datacenterId must be between 0 and " + MAX_DATACENTER_ID + " but was " + datacenterId);
        }
        this.epochMs = epochMs;
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = 0L;
        this.lastTimestamp = -1L;
    }

    /**
     * @return monotonically increasing unique id for this generator instance
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= MAX_BACKWARD_OFFSET_MS) {
                timestamp = waitUntilAfter(lastTimestamp);
            } else {
                throw new IllegalStateException(
                        "Clock moved backwards by " + offset + "ms; refusing to generate id");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = waitUntilAfter(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        long delta = timestamp - epochMs;
        if (delta < 0) {
            throw new IllegalStateException("Current time is before epoch; check epoch or system clock");
        }
        if (delta > (~(-1L << 41))) {
            throw new IllegalStateException("Timestamp overflow: epoch too old for 41-bit field");
        }

        return (delta << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    public String nextIdString() {
        return Long.toString(nextId());
    }

    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Block until {@code System.currentTimeMillis()} is strictly greater than
     * {@code pastMs}.
     */
    private static long waitUntilAfter(long pastMs) {
        long t = currentTimeMillis();
        while (t <= pastMs) {
            t = currentTimeMillis();
        }
        return t;
    }
}
