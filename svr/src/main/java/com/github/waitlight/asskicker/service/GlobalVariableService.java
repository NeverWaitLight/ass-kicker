package com.github.waitlight.asskicker.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.GlobalVariableEntity;
import com.github.waitlight.asskicker.repository.GlobalVariableRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalVariableService {

    private static final String ENABLED_VARIABLES_CACHE_KEY = "enabled";

    private final GlobalVariableRepository globalVariableRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<GlobalVariableEntity>> variableByIdCache;
    private AsyncLoadingCache<String, Optional<GlobalVariableEntity>> variableByKeyCache;
    private AsyncLoadingCache<String, Map<String, Object>> enabledVariablesCache;

    @PostConstruct
    void initCaches() {
        variableByIdCache = caffeineCacheConfig.buildCache((id, executor) -> globalVariableRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        variableByKeyCache = caffeineCacheConfig.buildCache((key, executor) -> globalVariableRepository.findByKey(key)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        enabledVariablesCache = caffeineCacheConfig.buildCache((key, executor) -> globalVariableRepository.findEnabled()
                .collect(LinkedHashMap<String, Object>::new,
                        (values, variable) -> values.put(variable.getKey(), variable.getValue()))
                .map(Map::copyOf)
                .toFuture());
    }

    public Mono<GlobalVariableEntity> create(GlobalVariableEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getKey())) {
            return Mono.error(new BadRequestException("globalVariable.key.empty"));
        }
        if (!StringUtils.hasText(entity.getName())) {
            return Mono.error(new BadRequestException("globalVariable.name.empty"));
        }
        if (!StringUtils.hasText(entity.getValue())) {
            return Mono.error(new BadRequestException("globalVariable.value.empty"));
        }

        normalize(entity);
        return globalVariableRepository.findByKey(entity.getKey())
                .flatMap(existing -> Mono.<GlobalVariableEntity>error(
                        new ConflictException("globalVariable.key.exists", entity.getKey())))
                .switchIfEmpty(Mono.defer(() -> {
                    GlobalVariableEntity toCreate = copyFieldsForCreate(entity);
                    long now = Instant.now().toEpochMilli();
                    toCreate.setCreatedAt(now);
                    toCreate.setUpdatedAt(now);
                    return globalVariableRepository.save(toCreate)
                            .doOnSuccess(saved -> invalidateVariableCaches(saved, saved));
                }));
    }

    public Mono<GlobalVariableEntity> update(String id, GlobalVariableEntity entity) {
        if (!StringUtils.hasText(id) || entity == null) {
            return Mono.error(new BadRequestException("globalVariable.id.empty"));
        }
        if (entity.getKey() != null && !StringUtils.hasText(entity.getKey())) {
            return Mono.error(new BadRequestException("globalVariable.key.empty"));
        }
        if (entity.getName() != null && !StringUtils.hasText(entity.getName())) {
            return Mono.error(new BadRequestException("globalVariable.name.empty"));
        }
        if (entity.getValue() != null && !StringUtils.hasText(entity.getValue())) {
            return Mono.error(new BadRequestException("globalVariable.value.empty"));
        }

        return globalVariableRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("globalVariable.id.notFound", id)))
                .flatMap(existing -> ensureKeyAvailable(entity, existing)
                        .then(Mono.defer(() -> {
                            String oldKey = existing.getKey();
                            mergePatch(entity, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            GlobalVariableEntity beforeSave = new GlobalVariableEntity();
                            beforeSave.setId(existing.getId());
                            beforeSave.setKey(oldKey);
                            return globalVariableRepository.save(existing)
                                    .doOnSuccess(saved -> invalidateVariableCaches(beforeSave, saved));
                        })));
    }

    public Mono<GlobalVariableEntity> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("globalVariable.id.empty"));
        }
        return Mono.defer(() -> Mono.fromFuture(variableByIdCache.get(id)))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("globalVariable.id.notFound", id))));
    }

    public Mono<GlobalVariableEntity> findByKey(String key) {
        if (!StringUtils.hasText(key)) {
            return Mono.error(new BadRequestException("globalVariable.key.empty"));
        }
        String normalizedKey = key.trim();
        return Mono.defer(() -> Mono.fromFuture(variableByKeyCache.get(normalizedKey)))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("globalVariable.key.notFound", key))));
    }

    public Mono<Map<String, Object>> findEnabledVariablesMap() {
        return Mono.defer(() -> Mono.fromFuture(enabledVariablesCache.get(ENABLED_VARIABLES_CACHE_KEY)));
    }

    public Mono<Long> count(String keyword) {
        return globalVariableRepository.count(keyword);
    }

    public Flux<GlobalVariableEntity> list(String keyword, int limit, int offset) {
        return globalVariableRepository.list(keyword, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return globalVariableRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("globalVariable.id.notFound", id)))
                .flatMap(variable -> globalVariableRepository.deleteById(variable.getId())
                        .doOnSuccess(v -> invalidateVariableCaches(variable, variable)));
    }

    private GlobalVariableEntity copyFieldsForCreate(GlobalVariableEntity source) {
        GlobalVariableEntity copy = new GlobalVariableEntity();
        copy.setKey(source.getKey());
        copy.setName(source.getName());
        copy.setValue(source.getValue());
        copy.setDescription(source.getDescription());
        copy.setEnabled(source.getEnabled() == null ? Boolean.TRUE : source.getEnabled());
        return copy;
    }

    private void mergePatch(GlobalVariableEntity patch, GlobalVariableEntity target) {
        if (StringUtils.hasText(patch.getKey())) {
            target.setKey(patch.getKey().trim());
        }
        if (patch.getName() != null) {
            target.setName(patch.getName().trim());
        }
        if (patch.getValue() != null) {
            target.setValue(patch.getValue());
        }
        if (patch.getDescription() != null) {
            target.setDescription(patch.getDescription());
        }
        if (patch.getEnabled() != null) {
            target.setEnabled(patch.getEnabled());
        }
    }

    private Mono<Void> ensureKeyAvailable(GlobalVariableEntity entity, GlobalVariableEntity existing) {
        String newKey = entity.getKey();
        if (!StringUtils.hasText(newKey) || newKey.trim().equals(existing.getKey())) {
            return Mono.empty();
        }
        entity.setKey(newKey.trim());
        return globalVariableRepository.findByKey(entity.getKey())
                .filter(found -> !found.getId().equals(existing.getId()))
                .flatMap(found -> Mono.<Void>error(new ConflictException("globalVariable.key.exists", entity.getKey())));
    }

    private void normalize(GlobalVariableEntity entity) {
        entity.setKey(entity.getKey().trim());
        entity.setName(entity.getName().trim());
    }

    private void invalidateVariableCaches(GlobalVariableEntity existing, GlobalVariableEntity saved) {
        variableByIdCache.synchronous().invalidate(saved.getId());
        variableByKeyCache.synchronous().invalidate(existing.getKey());
        variableByKeyCache.synchronous().invalidate(saved.getKey());
        enabledVariablesCache.synchronous().invalidate(ENABLED_VARIABLES_CACHE_KEY);
    }
}
