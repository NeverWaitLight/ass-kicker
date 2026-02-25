package com.github.waitlight.asskicker.testsend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TestSendRateLimiter {

    private final TestSendProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<Long, RateWindow> windows = new ConcurrentHashMap<>();

    @Autowired
    public TestSendRateLimiter(TestSendProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public TestSendRateLimiter(TestSendProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean tryAcquire(long userId) {
        long now = clock.millis();
        Duration windowDuration = properties.getWindow();
        long windowMillis = windowDuration == null ? 0 : windowDuration.toMillis();
        RateWindow window = windows.compute(userId, (id, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                return new RateWindow(now);
            }
            return existing;
        });
        int count = window.counter.incrementAndGet();
        if (count > properties.getMaxRequests()) {
            window.counter.decrementAndGet();
            return false;
        }
        return true;
    }

    public int getMaxRequests() {
        return properties.getMaxRequests();
    }

    public Duration getWindow() {
        return properties.getWindow();
    }

    static final class RateWindow {
        private final long windowStartMillis;
        private final AtomicInteger counter = new AtomicInteger();

        private RateWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
