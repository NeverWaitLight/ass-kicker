package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.repository.MessageTemplateRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTemplateService {

    private final MessageTemplateRepository messageTemplateRepository;
    private final MessageTemplateConverter messageTemplateConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByIdCache;
    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByCodeCache;

    @PostConstruct
    void initCaches() {
        templateByIdCache = caffeineCacheConfig.buildCache((id, executor) -> messageTemplateRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        templateByCodeCache = caffeineCacheConfig
                .buildCache((code, executor) -> messageTemplateRepository.findByCode(code)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    public Mono<TemplateEntity> create(TemplateEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getCode())) {
            return Mono.error(new BadRequestException("template.code.empty"));
        }

        return messageTemplateRepository.findByCode(entity.getCode())
                .flatMap(existing -> Mono.<TemplateEntity>error(new ConflictException("template.code.exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    TemplateEntity toCreate = messageTemplateConverter.copyForCreate(entity);
                    long now = Instant.now().toEpochMilli();
                    toCreate.setId(snowflakeIdGenerator.nextIdString());
                    toCreate.setCreatedAt(now);
                    toCreate.setUpdatedAt(now);
                    return messageTemplateRepository.save(toCreate)
                            .doOnSuccess(saved -> invalidateTemplateCaches(saved, saved));
                }));
    }

    public Mono<TemplateEntity> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.id.empty"));
        }

        return Mono.fromFuture(templateByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("template.id.notFound", id))));
    }

    public Mono<TemplateEntity> findByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return Mono.empty();
        }

        String key = code.trim();
        return Mono.fromFuture(templateByCodeCache.get(key))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("template.code.notFound", code))));
    }

    public Mono<TemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType) {
        return messageTemplateRepository.findByCodeAndChannelType(code, channelType);
    }

    public Flux<TemplateEntity> findByChannelType(ChannelType channelType) {
        return messageTemplateRepository.findByChannelType(channelType);
    }

    public Mono<Long> count(String keyword) {
        return messageTemplateRepository.count(keyword);
    }

    public Flux<TemplateEntity> list(String keyword, int limit, int offset) {
        return messageTemplateRepository.list(keyword, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return messageTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", id)))
                .flatMap(template -> messageTemplateRepository.deleteById(template.getId())
                        .doOnSuccess(v -> invalidateTemplateCaches(template, template)));
    }

    public Mono<TemplateEntity> update(String id, TemplateEntity entity) {
        if (entity == null || !StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.id.empty"));
        }

        return messageTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", id)))
                .flatMap(existing -> ensureCodeAvailable(entity, existing)
                        .then(Mono.defer(() -> {
                            messageTemplateConverter.merge(entity, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return messageTemplateRepository.save(existing)
                                    .doOnSuccess(saved -> invalidateTemplateCaches(existing, saved));
                        })));
    }

    private void invalidateTemplateCaches(TemplateEntity existing, TemplateEntity saved) {
        templateByIdCache.synchronous().invalidate(saved.getId());
        templateByCodeCache.synchronous().invalidate(existing.getCode());
        templateByCodeCache.synchronous().invalidate(saved.getCode());
    }

    private Mono<Void> ensureCodeAvailable(TemplateEntity entity, TemplateEntity existing) {
        String newCode = entity.getCode();
        if (!StringUtils.hasText(newCode) || newCode.trim().equals(existing.getCode())) {
            return Mono.empty();
        }
        entity.setCode(newCode.trim());
        return messageTemplateRepository.findByCode(entity.getCode())
                .filter(found -> !found.getId().equals(existing.getId()))
                .flatMap(found -> Mono.<Void>error(new ConflictException("template.code.exists")));
    }
}
