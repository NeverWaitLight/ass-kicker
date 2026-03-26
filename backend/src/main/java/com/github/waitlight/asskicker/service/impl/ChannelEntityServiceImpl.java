package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.repository.ChannelEntityRepository;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ChannelEntityService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class ChannelEntityServiceImpl implements ChannelEntityService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelEntityServiceImpl.class);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };
    private static final String ALL_CHANNELS_KEY = "all";
    private final ChannelEntityRepository channelEntityRepository;
    private final ObjectMapper objectMapper;
    private final CaffeineCacheConfig caffeineCacheConfig;
    private AsyncLoadingCache<String, Optional<ChannelEntity>> channelByIdCache;
    private AsyncLoadingCache<String, List<ChannelEntity>> channelListCache;

    public ChannelEntityServiceImpl(ChannelEntityRepository channelEntityRepository,
                                    ObjectMapper objectMapper,
                                    CaffeineCacheConfig caffeineCacheConfig) {
        this.channelEntityRepository = channelEntityRepository;
        this.objectMapper = objectMapper;
        this.caffeineCacheConfig = caffeineCacheConfig;
    }

    @PostConstruct
    void initCaches() {
        channelByIdCache = caffeineCacheConfig.buildCache((id, executor) ->
                channelEntityRepository.findById(id)
                        .map(this::enrichChannel)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());

        channelListCache = caffeineCacheConfig.buildCache((key, executor) ->
                channelEntityRepository.findAll()
                        .map(this::enrichChannel)
                        .collectList()
                        .toFuture());
    }

    @Override
    public Mono<ChannelEntity> createChannel(ChannelEntity channelEntity) {
        ChannelEntity toSave = new ChannelEntity();
        toSave.setId(null);
        toSave.setName(channelEntity.getName());
        toSave.setType(channelEntity.getType());
        toSave.setDescription(channelEntity.getDescription());
        toSave.setIncludeRecipientRegex(channelEntity.getIncludeRecipientRegex());
        toSave.setExcludeRecipientRegex(channelEntity.getExcludeRecipientRegex());
        long timestamp = Instant.now().toEpochMilli();
        toSave.setCreatedAt(timestamp);
        toSave.setUpdatedAt(timestamp);
        Map<String, Object> properties = normalizeProperties(channelEntity.getProperties());
        toSave.setPropertiesJson(writeProperties(properties));
        return channelEntityRepository.save(toSave)
                .map(this::enrichChannel)
                .doOnSuccess(saved -> channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY));
    }

    @Override
    public Mono<ChannelEntity> getChannelById(String id) {
        return Mono.fromFuture(channelByIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Flux<ChannelEntity> listChannels() {
        return Mono.fromFuture(channelListCache.get(ALL_CHANNELS_KEY))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<ChannelEntity> findByTypes(List<ChannelType> types) {
        return listChannels().filter(ch -> types.contains(ch.getType()));
    }

    @Override
    public Mono<ChannelEntity> updateChannel(String id, ChannelEntity channelEntity) {
        return channelEntityRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(channelEntity.getName());
                    existing.setType(channelEntity.getType());
                    existing.setDescription(channelEntity.getDescription());
                    existing.setIncludeRecipientRegex(channelEntity.getIncludeRecipientRegex());
                    existing.setExcludeRecipientRegex(channelEntity.getExcludeRecipientRegex());
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    Map<String, Object> properties = normalizeProperties(channelEntity.getProperties());
                    existing.setPropertiesJson(writeProperties(properties));
                    return channelEntityRepository.save(existing)
                            .doOnSuccess(saved -> {
                                channelByIdCache.synchronous().invalidate(id);
                                channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY);
                            });
                })
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Void> deleteChannel(String id) {
        return channelEntityRepository.deleteById(id)
                .doOnSuccess(v -> {
                    channelByIdCache.synchronous().invalidate(id);
                    channelListCache.synchronous().invalidate(ALL_CHANNELS_KEY);
                });
    }

    @Override
    public Mono<Object> testSend(TestSendRequest request, UserPrincipal principal) {
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
        // TODO: replace with the new channel send flow after migration.
        logger.info("SECURITY_TEST_SEND_PLACEHOLDER userId={} type={} protocol={} target={}",
                userId, request.type(), protocol, request.target());
        return Mono.just(Map.of(
                "success", false,
                "message", "TODO: 新发送逻辑尚未接入，当前为占位实现"));
    }

    private boolean hasTestSendPermission(UserPrincipal principal) {
        return principal.role() == UserRole.ADMIN || principal.role() == UserRole.USER;
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
        if ("WECOM".equals(upper)) return "WECOM";
        if ("APNS".equals(upper)) return "APNS";
        if ("FCM".equals(upper)) return "FCM";
        if ("ALIYUN".equals(upper)) return "ALIYUN";
        if ("TENCENT".equals(upper)) return "TENCENT";
        return upper;
    }

    private ChannelEntity enrichChannel(ChannelEntity channelEntity) {
        Map<String, Object> properties = readProperties(channelEntity.getPropertiesJson());
        channelEntity.setProperties(properties);
        return channelEntity;
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
