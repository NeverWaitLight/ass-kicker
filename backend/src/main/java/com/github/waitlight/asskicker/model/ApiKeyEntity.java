package com.github.waitlight.asskicker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_api_key")
public class ApiKeyEntity {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String name;

    @Field("key_hash")
    private String keyHash;

    @Field("key_prefix")
    private String keyPrefix;

    @Field("masked_raw_key")
    private String maskedRawKey;

    @Field("expires_at")
    private Long expiresAt;

    @Field("status")
    private ApiKeyStatus status;

    @Field("created_at")
    private Long createdAt;

    @Field("revoked_at")
    private Long revokedAt;
}
