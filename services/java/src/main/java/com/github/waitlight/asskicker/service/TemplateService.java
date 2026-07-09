package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.repository.LocalizedTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
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
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final LocalizedTemplateRepository localizedTemplateRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByIdCache;
    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByCodeCache;

    @PostConstruct
    void initCaches() {
        templateByIdCache = caffeineCacheConfig.buildCache((id, executor) -> templateRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        templateByCodeCache = caffeineCacheConfig
                .buildCache((code, executor) -> templateRepository.findByCode(code)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    public Mono<TemplateEntity> create(TemplateEntity entity, String userId) {
        if (entity == null || !StringUtils.hasText(entity.getCode())) {
            return Mono.error(new BadRequestException("template.code.empty"));
        }
        if (!StringUtils.hasText(entity.getName())) {
            return Mono.error(new BadRequestException("template.name.empty"));
        }

        return templateRepository.findByCode(entity.getCode())
                .flatMap(existing -> Mono.<TemplateEntity>error(new ConflictException("template.code.exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    long now = Instant.now().toEpochMilli();
                    entity.setCreator(userId);
                    entity.setUpdater(userId);
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    return templateRepository.save(entity)
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
        return templateRepository.findByCodeAndChannelType(code, channelType);
    }

    public Flux<TemplateEntity> findByChannelType(ChannelType channelType) {
        return templateRepository.findByChannelType(channelType);
    }

    public Mono<Long> count(String keyword, ChannelType channelType) {
        return templateRepository.count(keyword, channelType);
    }

    public Flux<TemplateEntity> list(String keyword, ChannelType channelType, int limit, int offset) {
        return templateRepository.list(keyword, channelType, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return templateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", id)))
                .flatMap(template -> localizedTemplateRepository.deleteByTemplateId(template.getId())
                        .then(templateRepository.deleteById(template.getId()))
                        .doOnSuccess(v -> invalidateTemplateCaches(template, template)));
    }

    public Mono<TemplateEntity> update(String id, TemplateEntity entity, String userId) {
        if (entity == null || !StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.id.empty"));
        }

        return templateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", id)))
                .flatMap(existing -> ensureCodeAvailable(entity, existing)
                        .then(Mono.defer(() -> {
                            applyUpdate(entity, existing);
                            existing.setUpdater(userId);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return templateRepository.save(existing)
                                    .doOnSuccess(saved -> invalidateTemplateCaches(existing, saved));
                        })));
    }

    /**
     * 用入参 entity 全量覆盖 target 的业务字段，保留 id、creator、createdAt
     */
    private void applyUpdate(TemplateEntity source, TemplateEntity target) {
        target.setCode(source.getCode());
        target.setName(source.getName().trim());
        target.setChannelType(source.getChannelType());
        target.setProviderManaged(source.isProviderManaged());
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
        return templateRepository.findByCode(entity.getCode())
                .filter(found -> !found.getId().equals(existing.getId()))
                .flatMap(found -> Mono.<Void>error(new ConflictException("template.code.exists")));
    }
}
