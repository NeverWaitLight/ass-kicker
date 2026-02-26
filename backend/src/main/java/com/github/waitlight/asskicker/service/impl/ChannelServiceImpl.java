package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.MessageRequest;
import com.github.waitlight.asskicker.channels.MessageResponse;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.email.EmailChannelPropertyMapper;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelPropertyMapper;
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
    private final EmailChannelPropertyMapper emailChannelPropertyMapper;
    private final IMChannelFactory imChannelFactory;
    private final IMChannelPropertyMapper imChannelPropertyMapper;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              ObjectMapper objectMapper,
                              EmailChannelFactory emailChannelFactory,
                              EmailChannelPropertyMapper emailChannelPropertyMapper,
                              IMChannelFactory imChannelFactory,
                              IMChannelPropertyMapper imChannelPropertyMapper) {
        this.channelRepository = channelRepository;
        this.objectMapper = objectMapper;
        this.emailChannelFactory = emailChannelFactory;
        this.emailChannelPropertyMapper = emailChannelPropertyMapper;
        this.imChannelFactory = imChannelFactory;
        this.imChannelPropertyMapper = imChannelPropertyMapper;
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
    public Mono<MessageResponse> testSend(TestSendRequest request, UserPrincipal principal) {
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

    private Mono<MessageResponse> sendWithRequest(TestSendRequest request, String protocol) {
        return Mono.fromCallable(() -> {
            try {
                if (request.type() == ChannelType.EMAIL) {
                    ChannelConfig config = emailChannelPropertyMapper.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = emailChannelFactory.create(config);
                    MessageRequest messageRequest = MessageRequest.builder()
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
                        MessageResponse response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                if (request.type() == ChannelType.IM) {
                    ChannelConfig config = imChannelPropertyMapper.fromProperties(request.properties());
                    com.github.waitlight.asskicker.channels.Channel<?> channel = imChannelFactory.create(config);
                    MessageRequest messageRequest = MessageRequest.builder()
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
                        MessageResponse response = channel.send(messageRequest);
                        logger.info("SECURITY_TEST_SEND_PROVIDER_RESULT type={} protocol={} success={} messageId={} errorCode={}",
                                request.type(), protocol, response.isSuccess(), response.getMessageId(), response.getErrorCode());
                        return response;
                    } finally {
                        closeChannel(channel);
                    }
                }
                logger.info("SECURITY_TEST_SEND_SIMULATED type={} protocol={} target={}",
                        request.type(), protocol, request.target());
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
