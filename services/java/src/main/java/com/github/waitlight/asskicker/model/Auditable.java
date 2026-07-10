package com.github.waitlight.asskicker.model;

/**
 * 支持创建与更新审计的实体接口，在 {@link Creatable} 之上追加 updater/updatedAt。
 */
public interface Auditable extends Creatable {

    String getUpdater();

    void setUpdater(String updater);

    Long getUpdatedAt();

    void setUpdatedAt(Long updatedAt);
}
