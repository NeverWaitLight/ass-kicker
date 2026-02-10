package com.github.waitlight.asskicker.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.waitlight.asskicker.logging.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class RequestIdIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void generatesRequestIdWhenMissing() {
        String requestId = webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst(RequestIdFilter.REQUEST_ID_HEADER);

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
    }

    @Test
    void preservesRequestIdFromHeader() {
        String requestId = webTestClient.get()
                .uri("/health")
                .header(RequestIdFilter.REQUEST_ID_HEADER, "test-request-id")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst(RequestIdFilter.REQUEST_ID_HEADER);

        assertEquals("test-request-id", requestId);
    }

    @Test
    void requestIdIsPresentInMdcLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        String requestId = null;
        try {
            requestId = webTestClient.get()
                    .uri("/health")
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(String.class)
                    .getResponseHeaders()
                    .getFirst(RequestIdFilter.REQUEST_ID_HEADER);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }

        assertNotNull(requestId);
        List<ILoggingEvent> events = appender.list;
        assertFalse(events.isEmpty());
        ILoggingEvent event = events.get(0);
        String mdcRequestId = event.getMDCPropertyMap().get(RequestIdFilter.REQUEST_ID_KEY);
        assertEquals(requestId, mdcRequestId);
        assertTrue(event.getFormattedMessage().contains("Incoming request"));
    }
}
