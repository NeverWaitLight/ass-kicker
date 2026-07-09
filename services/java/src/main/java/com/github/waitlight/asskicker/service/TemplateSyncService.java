package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.LocalizedTemplateEntity;
import com.github.waitlight.asskicker.model.ProviderTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.LocalizedTemplateRepository;
import com.github.waitlight.asskicker.repository.ProviderTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import com.github.waitlight.asskicker.template.SyncContext;
import com.github.waitlight.asskicker.template.SyncResult;
import com.github.waitlight.asskicker.template.TemplateSynchronizer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TemplateSyncService {

    private final TemplateRepository templateRepository;
    private final LocalizedTemplateRepository localizedTemplateRepository;
    private final ProviderTemplateRepository providerTemplateRepository;
    private final ChannelRepository channelRepository;
    private final List<TemplateSynchronizer> synchronizers;

    private Map<SynchronizerKey, TemplateSynchronizer> synchronizerIndex;

    public TemplateSyncService(TemplateRepository templateRepository,
            LocalizedTemplateRepository localizedTemplateRepository,
            ProviderTemplateRepository providerTemplateRepository,
            ChannelRepository channelRepository,
            List<TemplateSynchronizer> synchronizers) {
        this.templateRepository = templateRepository;
        this.localizedTemplateRepository = localizedTemplateRepository;
        this.providerTemplateRepository = providerTemplateRepository;
        this.channelRepository = channelRepository;
        this.synchronizers = synchronizers;
    }

    @PostConstruct
    void init() {
        synchronizerIndex = synchronizers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> new SynchronizerKey(s.type(), s.provider()),
                        s -> s,
                        (a, b) -> {
                            log.warn("Duplicate TemplateSynchronizer for {}/{}: {} kept, {} ignored",
                                    a.type(), a.provider(), a.getClass().getName(), b.getClass().getName());
                            return a;
                        }));
        log.info("Loaded {} TemplateSynchronizer(s)", synchronizerIndex.size());
    }

    /**
     * 触发本地模板同步：将 LocalizedTemplateEntity 推送到指定服务商，并回写 ProviderTemplateEntity。
     */
    public Mono<ProviderTemplateEntity> sync(String localizedTemplateId, SyncSpec spec, String userId) {
        if (!StringUtils.hasText(localizedTemplateId)) {
            return Mono.error(new BadRequestException("template.localized.id.empty"));
        }
        if (spec == null || spec.provider() == null) {
            return Mono.error(new BadRequestException("template.sync.provider.empty"));
        }

        return localizedTemplateRepository.findById(localizedTemplateId)
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound", localizedTemplateId)))
                .flatMap(localized -> templateRepository.findById(localized.getTemplateId())
                        .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", localized.getTemplateId())))
                        .flatMap(template -> resolveChannel(template.getChannelType(), spec)
                                .flatMap(channel -> doSyncAndPersist(template, localized, channel, spec, userId))));
    }

    private Mono<ChannelEntity> resolveChannel(ChannelType type, SyncSpec spec) {
        if (StringUtils.hasText(spec.channelId())) {
            return channelRepository.findById(spec.channelId())
                    .switchIfEmpty(Mono.error(new NotFoundException("channel.notFound", spec.channelId())))
                    .flatMap(ch -> {
                        if (ch.getType() != type || ch.getProvider() != spec.provider()) {
                            return Mono.error(new BadRequestException("template.sync.channel.mismatch"));
                        }
                        if (!ch.isEnabled()) {
                            return Mono.error(new BadRequestException("template.sync.channel.disabled"));
                        }
                        return Mono.just(ch);
                    });
        }
        return channelRepository.findByChannelTypeAndEnabled(type, true)
                .filter(ch -> ch.getProvider() == spec.provider())
                .next()
                .switchIfEmpty(Mono.error(new NotFoundException("template.sync.channel.notFound",
                        type.name() + "/" + spec.provider().name())));
    }

    private Mono<ProviderTemplateEntity> doSyncAndPersist(TemplateEntity template,
            LocalizedTemplateEntity localized,
            ChannelEntity channel,
            SyncSpec spec,
            String userId) {
        TemplateSynchronizer synchronizer = synchronizerIndex
                .get(new SynchronizerKey(template.getChannelType(), spec.provider()));
        if (synchronizer == null) {
            return Mono.error(new BadRequestException("template.sync.unsupported",
                    template.getChannelType().name() + "/" + spec.provider().name()));
        }

        return providerTemplateRepository
                .findByLocalizedTemplateIdAndProvider(localized.getId(), spec.provider())
                .defaultIfEmpty(newProviderEntity(localized.getId(), spec.provider(), userId))
                .flatMap(existing -> {
                    SyncContext ctx = SyncContext.builder()
                            .template(template)
                            .localized(localized)
                            .channel(channel)
                            .existingProviderTemplateCode(existing.getProviderTemplateCode())
                            .smsTemplateType(spec.smsTemplateType())
                            .international(spec.international())
                            .remark(spec.remark())
                            .build();
                    return synchronizer.sync(ctx)
                            .flatMap(result -> persistSuccess(existing, result, userId))
                            .onErrorResume(err -> persistFailure(existing, err, userId));
                });
    }

    private ProviderTemplateEntity newProviderEntity(String localizedTemplateId, ChannelProvider provider,
            String userId) {
        long now = Instant.now().toEpochMilli();
        ProviderTemplateEntity entity = new ProviderTemplateEntity();
        entity.setLocalizedTemplateId(localizedTemplateId);
        entity.setProvider(provider);
        entity.setCreator(userId);
        entity.setUpdater(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private Mono<ProviderTemplateEntity> persistSuccess(ProviderTemplateEntity entity, SyncResult result,
            String userId) {
        long now = Instant.now().toEpochMilli();
        entity.setProviderTemplateCode(result.providerTemplateCode());
        entity.setUploadedAt(now);
        entity.setFailureReason(null);
        entity.setUpdater(userId);
        entity.setUpdatedAt(now);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
            entity.setCreator(userId);
        }
        return providerTemplateRepository.save(entity);
    }

    private Mono<ProviderTemplateEntity> persistFailure(ProviderTemplateEntity entity, Throwable error,
            String userId) {
        long now = Instant.now().toEpochMilli();
        entity.setFailureReason(error.getMessage());
        entity.setUpdater(userId);
        entity.setUpdatedAt(now);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
            entity.setCreator(userId);
        }
        return providerTemplateRepository.save(entity)
                .then(Mono.error(error instanceof SendException
                        ? error
                        : new SendException(error.getMessage(), error)));
    }

    public record SyncSpec(ChannelProvider provider,
            String channelId,
            Integer smsTemplateType,
            Boolean international,
            String remark) {
    }

    private record SynchronizerKey(ChannelType type, ChannelProvider provider) {
    }
}
