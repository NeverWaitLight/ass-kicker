package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "users")
@CompoundIndex(name = "uk_users_username_deleted_at", def = "{'username': 1, 'deleted_at': 1}", unique = true)
public class UserEntity {

    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("password")
    private String password;

    @Field("role")
    private UserRole role;

    @Field("status")
    private UserStatus status;

    @Field("created_at")
    private Long createdAt;

    @Field("updated_at")
    private Long updatedAt;

    @Field("last_login_at")
    private Long lastLoginAt;

    @Field("deleted_at")
    private Long deletedAt;
}
