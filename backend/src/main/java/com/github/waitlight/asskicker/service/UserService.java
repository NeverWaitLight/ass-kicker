package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.dto.user.*;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserView> createUser(CreateUserRequest request);

    Mono<UserView> registerUser(RegisterRequest request);

    Mono<UserView> getUserById(String id);

    Mono<UserPageResponse> listUsers(int page, int size, String keyword);

    Mono<Void> deleteUser(String id);

    Mono<UserView> resetPassword(String id, String newPassword);

    Mono<UserView> updateUsername(String id, UpdateUsernameRequest request);

    Mono<UserView> updatePassword(String id, UpdatePasswordRequest request);
}
