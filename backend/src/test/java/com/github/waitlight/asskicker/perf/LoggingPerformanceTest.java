package com.github.waitlight.asskicker.perf;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayOutputStream;

@EnabledIfSystemProperty(named = "perfTests", matches = "true")
class LoggingPerformanceTest {

    @Test
    void compareSyncAndAsyncLogging() {
        int messageCount = 20000;
        long syncNanos = runSyncLogging(messageCount);
        long asyncNanos = runAsyncLogging(messageCount);

        System.out.printf("Sync logging: %d ms%n", syncNanos / 1_000_000);
        System.out.printf("Async logging: %d ms%n", asyncNanos / 1_000_000);
    }

    private long runSyncLogging(int messageCount) {
        LoggerContext context = new LoggerContext();
        OutputStreamAppender<ILoggingEvent> appender = buildAppender(context);

        Logger logger = context.getLogger("perf-sync");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);

        long start = System.nanoTime();
        for (int i = 0; i < messageCount; i++) {
            logger.info("sync-log {}", i);
        }
        long elapsed = System.nanoTime() - start;

        appender.stop();
        context.stop();
        return elapsed;
    }

    private long runAsyncLogging(int messageCount) {
        LoggerContext context = new LoggerContext();
        OutputStreamAppender<ILoggingEvent> delegate = buildAppender(context);

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(context);
        asyncAppender.setQueueSize(1024);
        asyncAppender.addAppender(delegate);
        asyncAppender.start();

        Logger logger = context.getLogger("perf-async");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(asyncAppender);

        long start = System.nanoTime();
        for (int i = 0; i < messageCount; i++) {
            logger.info("async-log {}", i);
        }
        asyncAppender.stop();
        long elapsed = System.nanoTime() - start;

        delegate.stop();
        context.stop();
        return elapsed;
    }

    private OutputStreamAppender<ILoggingEvent> buildAppender(LoggerContext context) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n");
        encoder.start();

        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setOutputStream(new ByteArrayOutputStream());
        appender.start();
        return appender;
    }
}
