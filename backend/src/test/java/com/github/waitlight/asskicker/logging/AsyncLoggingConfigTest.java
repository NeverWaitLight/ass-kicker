package com.github.waitlight.asskicker.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AsyncLoggingConfigTest {

    @Test
    void rootLoggerUsesAsyncAppender() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
        Iterator<Appender<ILoggingEvent>> iterator = rootLogger.iteratorForAppenders();
        while (iterator.hasNext()) {
            appenders.add(iterator.next());
        }

        AsyncAppender asyncAppender = appenders.stream()
                .filter(appender -> appender instanceof AsyncAppender)
                .map(appender -> (AsyncAppender) appender)
                .findFirst()
                .orElse(null);

        assertNotNull(asyncAppender, "Root logger should use AsyncAppender");
        assertEquals(512, asyncAppender.getQueueSize());
    }
}
