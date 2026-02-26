package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_user")
public class User {

    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("password_hash")
    private String passwordHash;

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
}
