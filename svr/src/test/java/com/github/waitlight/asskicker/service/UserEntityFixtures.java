package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;

public final class UserEntityFixtures {

    private UserEntityFixtures() {
    }

    public static UserEntity memberUser() {
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public static UserEntity adminUser() {
        UserEntity user = new UserEntity();
        user.setUsername("adminuser");
        user.setPassword("adminpass123");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public static UserEntity disabledUser() {
        UserEntity user = new UserEntity();
        user.setUsername("disableduser");
        user.setPassword("disabledpass123");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.DISABLED);
        return user;
    }

    public static UserEntity userWithUsername(String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword("password123");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public static UserEntity userWithPassword(String password) {
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setPassword(password);
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}