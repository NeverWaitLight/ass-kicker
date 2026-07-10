package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "users")
@CompoundIndex(name = "uk_t_user_username_deleted_at", def = "{'username': 1, 'deleted_at': 1}", unique = true)
public class UserEntity extends Auditable {

    @Field("username")
    private String username;

    @Field("password")
    private String password;

    @Field("role")
    private UserRole role;

    @Field("status")
    private UserStatus status;

    @Field("last_login_at")
    private Long lastLoginAt;

    @Field("kicked_out_at")
    private Long kickedOutAt;

    @Field("deleted_at")
    private Long deletedAt;
}
