package com.github.waitlight.asskicker.testsend;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSendRateLimiterTest {

    @Test
    void respectsWindowLimit() {
        TestSendProperties properties = new TestSendProperties();
        properties.setMaxRequests(2);
        properties.setWindow(Duration.ofSeconds(1));

        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        TestSendRateLimiter limiter = new TestSendRateLimiter(properties, clock);

        assertTrue(limiter.tryAcquire(1L));
        assertTrue(limiter.tryAcquire(1L));
        assertFalse(limiter.tryAcquire(1L));

        clock.advance(Duration.ofSeconds(2));
        assertTrue(limiter.tryAcquire(1L));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}