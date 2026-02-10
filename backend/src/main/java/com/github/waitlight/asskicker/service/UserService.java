package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.dto.user.*;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserView> createUser(CreateUserRequest request);

    Mono<UserView> registerUser(RegisterRequest request);

    Mono<UserView> getUserById(Long id);

    Mono<UserPageResponse> listUsers(int page, int size, String keyword);

    Mono<Void> deleteUser(Long id);

    Mono<UserView> resetPassword(Long id, String newPassword);

    Mono<UserView> updateUsername(Long id, UpdateUsernameRequest request);

    Mono<UserView> updatePassword(Long id, UpdatePasswordRequest request);
}
