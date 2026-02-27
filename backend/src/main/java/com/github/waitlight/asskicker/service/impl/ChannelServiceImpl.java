package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channels.push.PushChannelFactory;
import com.github.waitlight.asskicker.channels.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channels.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.channels.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.service.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ChannelServiceImpl implements ChannelService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };

    private final ChannelRepository channelRepository;
    private final ObjectMapper objectMapper;
    private final EmailChannelFactory emailChannelFactory;
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelFactory imChannelFactory;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelFactory pushChannelFactory;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelFactory smsChannelFactory;
    private final SmsChannelConfigConverter smsChannelConfigConverter;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              ObjectMapper objectMapper,
                              EmailChannelFactory emailChannelFactory,
                              EmailChannelConfigConverter emailChannelConfigConverter,
                              IMChannelFactory imChannelFactory,
                              IMChannelConfigConverter imChannelConfigConverter,
                              PushChannelFactory pushChannelFactory,
                              PushChannelConfigConverter pushChannelConfigConverter,
                              SmsChannelFactory smsChannelFactory,
                              SmsChannelConfigConverter smsChannelConfigConverter) {
        this.channelRepository = channelRepository;
        this.objectMapper = objectMapper;
        this.emailChannelFactory = emailChannelFactory;
        this.emailChannelConfigConverter = emailChannelConfigConverter;
        this.imChannelFactory = imChannelFactory;
        this.imChannelConfigConverter = imChannelConfigConverter;
        this.pushChannelFactory = pushChannelFactory;
        this.pushChannelConfigConverter = pushChannelConfigConverter;
        this.smsChannelFactory = smsChannelFactory;
        this.smsChannelConfigConverter = smsChannelConfigConverter;
    }

    @Override
    public Mono<Channel> createChannel(Channel channel) {
        Channel toSave = new Channel();
        toSave.setId(null);
        toSave.setName(channel.getName());
        toSave.setType(channel.getType());
        toSave.setDescription(channel.getDescription());
        long timestamp = Instant.now().toEpochMilli();
        toSave.setCreatedAt(timestamp);
        toSave.setUpdatedAt(timestamp);
        Map<String, Object> properties = normalizeProperties(channel.getProperties());
        toSave.setPropertiesJson(writeProperties(properties));
        return channelRepository.save(toSave)
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Channel> getChannelById(String id) {
        return channelRepository.findById(id)
                .map(this::enrichChannel);
    }

    @Override
    public Flux<Channel> listChannels() {
        return channelRepository.findAll()
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Channel> updateChannel(String id, Channel channel) {
        return channelRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(channel.getName());
                    existing.setType(channel.getType());
                    existing.setDescription(channel.getDescription());
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    Map<String, Object> properties = normalizeProperties(channel.getProperties());
                    existing.setPropertiesJson(writeProperties(properties));
                    return channelRepository.save(existing);
                })
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Void> deleteChannel(String id) {
        return channelRepository.deleteById(id);
    }

    @Override
    public Mono<MsgResp> testSend(TestSendRequest request, UserPrincipal principal) {
        if (principal == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未授权"));
        }
        if (!hasTestSendPermission(principal)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "没有权限执行测试发送"));
        }
        String userId = principal.userId();
        String protocol = resolveProtocol(request.properties());
        logger.info("SECURITY_TEST_SEND_START userId={} type={} protocol={} target={}",
                userId, request.type(), protocol, request.target());
        return sendWithRequest(request, protocol)
                .doOnNext(response -> logger.info(
                        "SECURITY_TEST_SEND_END userId={} type={} protocol={} success={} messageId={}",
                        userId, request.type(), protocol, response.isSuccess(), response.getMessageId()))
                .doOnError(ex -> logger.warn(
                        "SECURITY_TEST_SEND_ERROR userId={} type={} protocol={} reason={}",
                        userId, request.type(), protocol, ex.getMessage()));
    }

    private boolean hasTestSendPermission(UserPrincipal principal) {
        return principal.role() == UserRole.ADMIN || principal.role() == UserRole.USER;
    }

    private Mono<MsgResp> sendWithRequest(TestSendRequest request, String protocol) {
        return Mono.fromCallable(() -> {
            try {
                if (request.type() == ChannelType.EMAIL) {
                    ChannelConfig config = emailChannelConfigConverter.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = emailChannelFactory.create(config);
                    MsgReq messageRequest = MsgReq.builder()
                            .recipient(request.target())
                            .subject("测试消息")
                            .content(request.content())
                            .attributes(Map.of("senderType", request.type().name()))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY type={} protocol={} sender={}",
                                request.type(), protocol, channel.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC type={} protocol={} target={}",
                                request.type(), protocol, request.target());
                        MsgResp response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                if (request.type() == ChannelType.IM) {
                    ChannelConfig config = imChannelConfigConverter.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = imChannelFactory.create(config);
                    MsgReq messageRequest = MsgReq.builder()
                            .recipient(request.target())
                            .subject("测试消息")
                            .content(request.content())
                            .attributes(Map.of("senderType", request.type().name()))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY type={} protocol={} sender={}",
                                request.type(), protocol, channel.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC type={} protocol={} target={}",
                                request.type(), protocol, request.target());
                        MsgResp response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                if (request.type() == ChannelType.PUSH) {
                    ChannelConfig config = pushChannelConfigConverter.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = pushChannelFactory.create(config);
                    MsgReq messageRequest = MsgReq.builder()
                            .recipient(request.target())
                            .subject("测试消息")
                            .content(request.content())
                            .attributes(Map.of("senderType", request.type().name()))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY type={} protocol={} sender={}",
                                request.type(), protocol, channel.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC type={} protocol={} target={}",
                                request.type(), protocol, request.target());
                        MsgResp response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                if (request.type() == ChannelType.SMS) {
                    ChannelConfig config = smsChannelConfigConverter.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = smsChannelFactory.create(config);
                    MsgReq messageRequest = MsgReq.builder()
                            .recipient(request.target())
                            .subject("")
                            .content(request.content())
                            .attributes(Map.of("senderType", request.type().name()))
                            .build();
                    try {
                        logger.info("SECURITY_TEST_SEND_SENDER_READY type={} protocol={} sender={}",
                                request.type(), protocol, channel.getClass().getSimpleName());
                        logger.info("SECURITY_TEST_SEND_EXEC type={} protocol={} target={}",
                                request.type(), protocol, request.target());
                        MsgResp response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                logger.info("SECURITY_TEST_SEND_SIMULATED type={} protocol={} target={}",
                        request.type(), protocol, request.target());
                return MsgResp.success("SIMULATED-" + UUID.randomUUID());
            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (Exception ex) {
                return MsgResp.failure("TEST_SEND_FAILED", ex.getMessage());
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
        if ("DINGTALK".equals(upper)) return "DINGTALK";
        if ("WECHAT_WORK".equals(upper)) return "WECHAT_WORK";
        if ("APNS".equals(upper)) return "APNS";
        if ("FCM".equals(upper)) return "FCM";
        if ("ALIYUN".equals(upper)) return "ALIYUN";
        if ("TENCENT".equals(upper)) return "TENCENT";
        return upper;
    }

    private void closeChannel(com.github.waitlight.asskicker.channels.Channel<?> channel) {
        if (channel instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                logger.warn("SECURITY_TEST_SEND_SENDER_CLOSE_FAILED reason={}", ex.getMessage());
            }
        }
    }

    private Channel enrichChannel(Channel channel) {
        Map<String, Object> properties = readProperties(channel.getPropertiesJson());
        channel.setProperties(properties);
        return channel;
    }

    private Map<String, Object> normalizeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(properties);
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            System.err.println("Failed to read channel properties: " + ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String writeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception ex) {
            System.err.println("Failed to write channel properties: " + ex.getMessage());
            return "{}";
        }
    }
}
