package com.github.waitlight.asskicker.model;

/**
 * 只记录创建者与创建时间、且创建后不可再修改的实体标记接口。
 * 典型场景：发送流水（RecordEntity）——一次落库、终态写入、不再更新。
 * 需要跟踪修改者的实体请实现 {@link Auditable}。
 */
public interface Creatable {

    String getId();

    String getCreator();

    void setCreator(String creator);

    Long getCreatedAt();

    void setCreatedAt(Long createdAt);
}
