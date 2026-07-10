package com.github.waitlight.asskicker.config;

import java.time.Instant;

import org.reactivestreams.Publisher;
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback;
import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.model.Auditable;
import com.github.waitlight.asskicker.model.Creatable;
import com.github.waitlight.asskicker.security.AuditorContext;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Spring Data 保存前回调，按实体接口层级自动填充审计字段。
 * <ul>
 *   <li>{@link Creatable}：首次保存时补 creator + createdAt。</li>
 *   <li>{@link Auditable}：在 Creatable 基础上，无论新旧都刷新 updater + updatedAt。</li>
 * </ul>
 * 拿不到当前用户时（例如 MQ 消费线程、匿名注册），仅补空的 creator/updater，不覆盖既有值。
 */
@Component
@Slf4j
public class AuditingCallback implements ReactiveBeforeConvertCallback<Object> {

    @Override
    public Publisher<Object> onBeforeConvert(Object entity, String collection) {
        if (!(entity instanceof Creatable creatable)) {
            return Mono.just(entity);
        }
        long now = Instant.now().toEpochMilli();
        boolean isCreate = creatable.getId() == null || creatable.getCreatedAt() == null;
        return AuditorContext.currentUserId()
                .map(opt -> {
                    String userId = opt.orElse(null);
                    if (isCreate) {
                        if (creatable.getCreator() == null && userId != null) {
                            creatable.setCreator(userId);
                        }
                        if (creatable.getCreatedAt() == null) {
                            creatable.setCreatedAt(now);
                        }
                    }
                    if (creatable instanceof Auditable auditable) {
                        if (userId != null) {
                            auditable.setUpdater(userId);
                        }
                        auditable.setUpdatedAt(now);
                    }
                    return (Object) creatable;
                });
    }
}
