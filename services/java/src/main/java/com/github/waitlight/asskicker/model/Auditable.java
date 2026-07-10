package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 支持创建与更新审计的实体基类，在 {@link Creatable} 之上追加 updater/updatedAt。
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class Auditable extends Creatable {

    @Field("updater")
    private String updater;

    @Field("updated_at")
    private Long updatedAt;
}
