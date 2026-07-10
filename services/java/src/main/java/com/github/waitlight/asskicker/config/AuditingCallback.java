package com.github.waitlight.asskicker.config;

import java.time.Instant;

import org.reactivestreams.Publisher;
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback;
import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.model.Auditable;
import com.github.waitlight.asskicker.security.AuditorContext;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 通过 Spring Data 的保存前回调，自动填充 Auditable 实体的 creator/updater/createdAt/updatedAt。
 * 首次保存（id 为空或 createdAt 未设置）视为 create，会填充 creator + createdAt；
 * 其余保存视为 update，只刷新 updater + updatedAt。
 */
@Component
@Slf4j
public class AuditingCallback implements ReactiveBeforeConvertCallback<Object> {

    @Override
    public Publisher<Object> onBeforeConvert(Object entity, String collection) {
        if (!(entity instanceof Auditable auditable)) {
            return Mono.just(entity);
        }
        long now = Instant.now().toEpochMilli();
        boolean isCreate = auditable.getId() == null || auditable.getCreatedAt() == null;
        return AuditorContext.currentUserId()
                .map(opt -> {
                    String userId = opt.orElse(null);
                    if (isCreate) {
                        if (auditable.getCreator() == null && userId != null) {
                            auditable.setCreator(userId);
                        }
                        if (auditable.getCreatedAt() == null) {
                            auditable.setCreatedAt(now);
                        }
                    }
                    if (userId != null) {
                        auditable.setUpdater(userId);
                    }
                    auditable.setUpdatedAt(now);
                    return (Object) auditable;
                });
    }
}
