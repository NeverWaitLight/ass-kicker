package com.github.waitlight.asskicker.perf;

import com.github.waitlight.asskicker.testsend.TestSendProperties;
import com.github.waitlight.asskicker.testsend.TestSendRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@EnabledIfSystemProperty(named = "perfTests", matches = "true")
class TestSendRateLimiterPerformanceTest {

    @Test
    void benchmarkRateLimiter() {
        TestSendProperties properties = new TestSendProperties();
        properties.setMaxRequests(1_000_000);
        properties.setWindow(Duration.ofMinutes(10));

        TestSendRateLimiter limiter = new TestSendRateLimiter(properties, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        long start = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            limiter.tryAcquire(1L);
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("Rate limiter 1,000,000 ops: %d ms%n", elapsed / 1_000_000);
    }
}