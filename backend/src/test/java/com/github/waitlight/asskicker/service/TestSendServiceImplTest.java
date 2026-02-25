package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.SenderConfig;
import com.github.waitlight.asskicker.sender.email.EmailSenderFactory;
import com.github.waitlight.asskicker.sender.email.SmtpEmailSenderConfig;
import com.github.waitlight.asskicker.sender.email.EmailSender;
import com.github.waitlight.asskicker.sender.email.EmailSenderPropertyMapper;
import com.github.waitlight.asskicker.service.impl.TestSendServiceImpl;
import com.github.waitlight.asskicker.testsend.TemporaryChannelConfigManager;
import com.github.waitlight.asskicker.testsend.TestSendProperties;
import com.github.waitlight.asskicker.testsend.TestSendRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSendServiceImplTest {

    @Test
    void sendsEmailThroughService() {
        TestSendProperties properties = new TestSendProperties();
        TestSendRateLimiter limiter = new TestSendRateLimiter(properties, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        TemporaryChannelConfigManager manager = new TemporaryChannelConfigManager();
        EmailSenderFactory factory = new EmailSenderFactory() {
            @Override
            public EmailSender<?> create(SenderConfig config) {
                return new EmailSender<>(new SmtpEmailSenderConfig()) {
                    @Override
                    public MessageResponse send(com.github.waitlight.asskicker.sender.MessageRequest request) {
                        return MessageResponse.success("id-1");
                    }
                };
            }
        };
        EmailSenderPropertyMapper mapper = new EmailSenderPropertyMapper();
        TestSendServiceImpl service = new TestSendServiceImpl(factory, mapper, manager, limiter);

        Map<String, Object> smtp = Map.of(
                "host", "smtp.example.com",
                "port", "465",
                "username", "user@example.com",
                "password", "pass"
        );
        TestSendRequest request = new TestSendRequest(ChannelType.EMAIL,
                Map.of("protocol", "SMTP", "smtp", smtp),
                "test@example.com",
                "hello");
        UserPrincipal principal = new UserPrincipal(1L, UserRole.USER);

        StepVerifier.create(service.testSend(request, principal))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("id-1", response.getMessageId());
                })
                .verifyComplete();
    }

    @Test
    void rateLimitsTestSend() {
        TestSendProperties properties = new TestSendProperties();
        properties.setMaxRequests(1);
        properties.setWindow(Duration.ofMinutes(1));

        TestSendRateLimiter limiter = new TestSendRateLimiter(properties, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        TemporaryChannelConfigManager manager = new TemporaryChannelConfigManager();
        EmailSenderFactory factory = new EmailSenderFactory() {
            @Override
            public EmailSender<?> create(SenderConfig config) {
                return new EmailSender<>(new SmtpEmailSenderConfig()) {
                    @Override
                    public MessageResponse send(com.github.waitlight.asskicker.sender.MessageRequest request) {
                        return MessageResponse.success("id-1");
                    }
                };
            }
        };
        EmailSenderPropertyMapper mapper = new EmailSenderPropertyMapper();
        TestSendServiceImpl service = new TestSendServiceImpl(factory, mapper, manager, limiter);

        Map<String, Object> smtp = Map.of(
                "host", "smtp.example.com",
                "port", "465",
                "username", "user@example.com",
                "password", "pass"
        );
        TestSendRequest request = new TestSendRequest(ChannelType.EMAIL,
                Map.of("protocol", "SMTP", "smtp", smtp),
                "test@example.com",
                "hello");
        UserPrincipal principal = new UserPrincipal(2L, UserRole.USER);

        StepVerifier.create(service.testSend(request, principal))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(service.testSend(request, principal))
                .expectErrorMatches(ex -> {
                    if (!(ex instanceof ResponseStatusException statusException)) {
                        return false;
                    }
                    return statusException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
                })
                .verify();
    }
}
