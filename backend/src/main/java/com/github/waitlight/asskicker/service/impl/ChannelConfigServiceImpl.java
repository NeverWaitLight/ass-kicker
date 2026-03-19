package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.channel.MsgReq;
import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelConfig;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.channel.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channel.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channel.im.IMChannelFactory;
import com.github.waitlight.asskicker.channel.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channel.push.PushChannelFactory;
import com.github.waitlight.asskicker.channel.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channel.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.channel.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.repository.ChannelConfigRepository;
import com.github.waitlight.asskicker.service.ChannelConfigService;
import jakarta.annotation.PostConstruct;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChannelConfigServiceImpl implements ChannelConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelConfigServiceImpl.class);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };

    private final ChannelConfigRepository channelConfigRepository;
    private final ObjectMapper objectMapper;
    private final EmailChannelFactory emailChannelFactory;
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelFactory imChannelFactory;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelFactory pushChannelFactory;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelFactory smsChannelFactory;
    private final SmsChannelConfigConverter smsChannelConfigConverter;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<ChannelConfig>> channelByIdCache;
    private AsyncLoadingCache<String, List<ChannelConfig>> channelListCache;

    private static final String ALL_CHANNELS_KEY = "all";

    public ChannelConfigServiceImpl(ChannelConfigRepository channelConfigRepository,
                                    ObjectMapper objectMapper,
                                    EmailChannelFactory emailChannelFactory,
                                    EmailChannelConfigConverter emailChannelConfigConverter,
                                    IMChannelFactory imChannelFactory,
                                    IMChannelConfigConverter imChannelConfigConverter,
                                    PushChannelFactory pushChannelFactory,
                                    PushChannelConfigConverter pushChannelConfigConverter,
                                    SmsChannelFactory smsChannelFactory,
                                    SmsChannelConfigConverter smsChannelConfigConverter,
                                    CaffeineCacheConfig caffeineCacheConfig) {
        this.channelConfigRepository = channelConfigRepository;
        this.objectMapper = objectMapper;
        this.emailChannelFactory = emailChannelFactory;
        this.emailChannelConfigConverter = emailChannelConfigConverter;
        this.imChannelFactory = imChannelFactory;
        this.imChannelConfigConverter = imChannelConfigConverter;
        this.pushChannelFactory = pushChannelFactory;
        this.pushChannelConfigConverter = pushChannelConfigConverter;
        this.smsChannelFactory = smsChannelFactory;
        this.smsChannelConfigConverter = smsChannelConfigConverter;
        this.caffeineCacheConfig = caffeineCacheConfig;
    }

    @PostConstruct
    void initCaches() {
        channelByIdCache = caffeineCacheConfig.buildCache((id, executor) ->
                channelConfigRepository.findById(id)
                        .map(this::enrichChannel)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());

        channelListCache = caffeineCacheConfig.buildCache((key, executor) ->
                channelConfigRepository.findAll()
                        .map(this::enrichChannel)
                        .collectList()
                        .toFuture());
    }

    @Override
    public Mono<ChannelConfig> createChannel(ChannelConfig channelConfig) {
        ChannelConfig toSave = new ChannelConfig();
        toSave.setId(null);
        toSave.setName(channelConfig.getName());
        toSave.setType(channelConfig.getType());
        toSave.setDescription(channelConfig.getDescription());
        long timestamp = Instant.now().toEpochMilli();
        toSave.setCreatedAt(timestamp);
        toSave.setUpdatedAt(timestamp);
        Map<String, Object> properties = normalizeProperties(channelConfig.getProperties());
        toSave.setPropertiesJson(writeProperties(properties));
        return channelConfigRepository.save(toSave)
                .map(this::enrichChannel)
                .doOnSuccess(saved -> channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY));
    }

    @Override
    public Mono<ChannelConfig> getChannelById(String id) {
        return Mono.fromFuture(channelByIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Flux<ChannelConfig> listChannels() {
        return Mono.fromFuture(channelListCache.get(ALL_CHANNELS_KEY))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<ChannelConfig> findByTypes(List<ChannelType> types) {
        return listChannels().filter(ch -> types.contains(ch.getType()));
    }

    @Override
    public Mono<ChannelConfig> updateChannel(String id, ChannelConfig channelConfig) {
        return channelConfigRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(channelConfig.getName());
                    existing.setType(channelConfig.getType());
                    existing.setDescription(channelConfig.getDescription());
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    Map<String, Object> properties = normalizeProperties(channelConfig.getProperties());
                    existing.setPropertiesJson(writeProperties(properties));
                    return channelConfigRepository.save(existing)
                            .doOnSuccess(saved -> {
                                channelByIdCache.synchronous().invalidate(id);
                                channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY);
                            });
                })
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Void> deleteChannel(String id) {
        return channelConfigRepository.deleteById(id)
                .doOnSuccess(v -> {
                    channelByIdCache.synchronous().invalidate(id);
                    channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY);
                });
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
                    ChannelProperties config = emailChannelConfigConverter.fromProperties(request.properties());
                    Channel<?> channel = emailChannelFactory.create(config);
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
                    ChannelProperties config = imChannelConfigConverter.fromProperties(request.properties());
                    Channel<?> channel = imChannelFactory.create(config);
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
                    ChannelProperties config = pushChannelConfigConverter.fromProperties(request.properties());
                    Channel<?> channel = pushChannelFactory.create(config);
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
                    ChannelProperties config = smsChannelConfigConverter.fromProperties(request.properties());
                    Channel<?> channel = smsChannelFactory.create(config);
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

    private void closeChannel(Channel<?> channel) {
        if (channel instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                logger.warn("SECURITY_TEST_SEND_SENDER_CLOSE_FAILED reason={}", ex.getMessage());
            }
        }
    }

    private ChannelConfig enrichChannel(ChannelConfig channelConfig) {
        Map<String, Object> properties = readProperties(channelConfig.getPropertiesJson());
        channelConfig.setProperties(properties);
        return channelConfig;
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
