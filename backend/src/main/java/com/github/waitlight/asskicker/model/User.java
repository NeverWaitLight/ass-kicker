package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@Table("t_user")
public class User {

    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("password_hash")
    private String passwordHash;

    @Column("role")
    private UserRole role;

    @Column("status")
    private UserStatus status;

    @Column("created_at")
    private Long createdAt;

    @Column("updated_at")
    private Long updatedAt;

    @Column("last_login_at")
    private Long lastLoginAt;
}
