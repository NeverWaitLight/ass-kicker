package com.github.waitlight.asskicker.model;

public interface Auditable {

    String getCreator();

    void setCreator(String creator);

    String getUpdater();

    void setUpdater(String updater);

    Long getCreatedAt();

    void setCreatedAt(Long createdAt);

    Long getUpdatedAt();

    void setUpdatedAt(Long updatedAt);

    String getId();
}
