package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.dto.sender.TestSendRequest;
import com.github.waitlight.asskicker.model.SenderType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.Sender;
import com.github.waitlight.asskicker.sender.SenderConfig;
import com.github.waitlight.asskicker.sender.email.EmailSenderFactory;
import com.github.waitlight.asskicker.sender.email.EmailSenderPropertyMapper;
import com.github.waitlight.asskicker.sender.im.IMSenderFactory;
import com.github.waitlight.asskicker.sender.im.IMSenderPropertyMapper;
import com.github.waitlight.asskicker.service.TestSendService;
import com.github.waitlight.asskicker.testsend.TemporarySenderConfig;
import com.github.waitlight.asskicker.testsend.TemporarySenderConfigManager;
import com.github.waitlight.asskicker.testsend.TestSendRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TestSendServiceImpl implements TestSendService {

    private static final Logger logger = LoggerFactory.getLogger(TestSendServiceImpl.class);

    private final EmailSenderFactory emailSenderFactory;
    private final EmailSenderPropertyMapper emailSenderPropertyMapper;
    private final IMSenderFactory imSenderFactory;
    private final IMSenderPropertyMapper imSenderPropertyMapper;
    private final TemporarySenderConfigManager configManager;
    private final TestSendRateLimiter rateLimiter;

    public TestSendServiceImpl(EmailSenderFactory emailSenderFactory,
                               EmailSenderPropertyMapper emailSenderPropertyMapper,
                               IMSenderFactory imSenderFactory,
                               IMSenderPropertyMapper imSenderPropertyMapper,
                               TemporarySenderConfigManager configManager,
                               TestSendRateLimiter rateLimiter) {
        this.emailSenderFactory = emailSenderFactory;
        this.emailSenderPropertyMapper = emailSenderPropertyMapper;
        this.imSenderFactory = imSenderFactory;
        this.imSenderPropertyMapper = imSenderPropertyMapper;
        this.configManager = configManager;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<MessageResponse> testSend(TestSendRequest request, UserPrincipal principal) {
        if (principal == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未授权"));
        }
        if (!hasPermission(principal)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "没有权限执行测试发送"));
        }
        String userId = principal.userId();
        if (!rateLimiter.tryAcquire(userId)) {
            logger.warn("SECURITY_TEST_SEND_RATE_LIMIT userId={} window={} max={}",
                    userId,
                    rateLimiter.getWindow(),
                    rateLimiter.getMaxRequests());
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "测试发送过于频繁，请稍后再试"));
        }
        return Mono.defer(() -> {
            TemporarySenderConfig config = configManager.create(request.type(), request.properties());
            String protocol = resolveProtocol(config.properties());
            logger.info("SECURITY_TEST_SEND_START userId={} configId={} type={} protocol={} target={}",
                    userId, config.id(), config.type(), protocol, request.target());
            return sendWithConfig(config, request, protocol)
                    .doOnNext(response -> logger.info(
                            "SECURITY_TEST_SEND_END userId={} configId={} type={} protocol={} success={} messageId={}",
                            userId,
                            config.id(),
                            config.type(),
                            protocol,
                            response.isSuccess(),
                            response.getMessageId()))
                    .doOnError(ex -> logger.warn(
                            "SECURITY_TEST_SEND_ERROR userId={} configId={} type={} protocol={} reason={}",
                            userId,
                            config.id(),
                            config.type(),
                            protocol,
                            ex.getMessage()))
                    .doFinally(signal -> configManager.remove(config.id()));
        });
    }

    private boolean hasPermission(UserPrincipal principal) {
        return principal.role() == UserRole.ADMIN || principal.role() == UserRole.USER;
    }

    private Mono<MessageResponse> sendWithConfig(TemporarySenderConfig config, TestSendRequest request, String protocol) {
        return Mono.fromCallable(() -> {
            try {
                if (config.type() == SenderType.EMAIL) {
                    SenderConfig property = emailSenderPropertyMapper.fromProperties(config.properties());
                    Sender emailSender = emailSenderFactory.create(property);
                    MessageRequest messageRequest = MessageRequest.builder()
                            .recipient(request.target())
                            .subject("测试消息")
                            .content(request.content())
                            .attributes(Map.of(
                                    "temporaryConfigId", config.id(),
                                    "senderType", config.type().name()
                            ))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY configId={} protocol={} sender={}",
                                config.id(), protocol, emailSender.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC configId={} protocol={} target={}",
                                config.id(), protocol, request.target());
                        MessageResponse response = emailSender.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT configId={} protocol={} success={} messageId={} errorCode={}",
                                config.id(),
                                protocol,
                                response.isSuccess(),
                                response.getMessageId(),
                                response.getErrorCode());
                        return response;
                    } finally {
                        closeSender(emailSender);
                    }
                } else if (config.type() == SenderType.IM) {
                    SenderConfig property = imSenderPropertyMapper.fromProperties(config.properties());
                    Sender imSender = imSenderFactory.create(property);
                    MessageRequest messageRequest = MessageRequest.builder()
                            .recipient(request.target())
                            .subject("测试消息")
                            .content(request.content())
                            .attributes(Map.of(
                                    "temporaryConfigId", config.id(),
                                    "senderType", config.type().name()
                            ))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY configId={} protocol={} sender={}",
                                config.id(), protocol, imSender.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC configId={} protocol={} target={}",
                                config.id(), protocol, request.target());
                        MessageResponse response = imSender.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT configId={} protocol={} success={} messageId={} errorCode={}",
                                config.id(),
                                protocol,
                                response.isSuccess(),
                                response.getMessageId(),
                                response.getErrorCode());
                        return response;
                    } finally {
                        closeSender(imSender);
                    }
                }
                logger.info("SECURITY_TEST_SEND_SIMULATED type={} protocol={} target={}",
                        config.type(), protocol, request.target());
                return MessageResponse.success("SIMULATED-" + UUID.randomUUID());
            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (Exception ex) {
                return MessageResponse.failure("TEST_SEND_FAILED", ex.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String resolveProtocol(Map<String, Object> properties) {
        if (properties == null) {
            return "SMTP";
        }
        Object value = properties.get("type");
        if (value == null || String.valueOf(value).trim().isBlank()) {
            value = properties.get("protocol");
        }
        if (value == null) {
            return "SMTP";
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return "SMTP";
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if ("DINGTALK".equals(upper)) {
            return "DINGTALK";
        }
        return "SMTP";
    }

    private void closeSender(Sender sender) {
        if (sender instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                logger.warn("SECURITY_TEST_SEND_SENDER_CLOSE_FAILED reason={}", ex.getMessage());
            }
        }
    }
}
