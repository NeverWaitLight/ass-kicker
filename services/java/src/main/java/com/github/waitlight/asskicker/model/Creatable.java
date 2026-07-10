package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 只记录创建者与创建时间、且创建后不可再修改的实体基类。
 * 典型场景：发送流水（RecordEntity）——一次落库、终态写入、不再更新。
 * 需要跟踪修改者的实体请继承 {@link Auditable}。
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class Creatable {

    @Id
    private String id;

    @Field("creator")
    private String creator;

    @Field("created_at")
    private Long createdAt;
}
