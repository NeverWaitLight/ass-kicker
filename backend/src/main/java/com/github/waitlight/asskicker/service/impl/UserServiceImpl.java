package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.dto.user.*;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.RegistrationLock;
import com.github.waitlight.asskicker.repository.UserQueryRepository;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationLock registrationLock;
    private final UserConverter userMapStructer;

    public UserServiceImpl(UserRepository userRepository, UserQueryRepository userQueryRepository, PasswordEncoder passwordEncoder, RegistrationLock registrationLock, UserConverter userMapStructer) {
        this.userRepository = userRepository;
        this.userQueryRepository = userQueryRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationLock = registrationLock;
        this.userMapStructer = userMapStructer;
    }

    @Override
    public Mono<UserView> createUser(CreateUserRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }
        String username = request.username().trim();
        return userRepository.existsByUsername(username)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在"));
                    }
                    User user = new User();
                    long now = Instant.now().toEpochMilli();
                    user.setUsername(username);
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    user.setRole(request.role() == null ? UserRole.USER : request.role());
                    user.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    return userRepository.save(user).map(userMapStructer::toView);
                });
    }

    @Override
    @Transactional
    public Mono<UserView> registerUser(RegisterRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }
        String username = request.username().trim();
        return registrationLock.acquire()
                .then(userRepository.existsByUsername(username))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在"));
                    }
                    return userRepository.count();
                })
                .flatMap(count -> {
                    User user = new User();
                    long now = Instant.now().toEpochMilli();
                    user.setUsername(username);
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    user.setRole(count == 0 ? UserRole.ADMIN : UserRole.USER);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    return userRepository.save(user).map(userMapStructer::toView);
                });
    }

    @Override
    public Mono<UserView> getUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .map(userMapStructer::toView);
    }

    @Override
    public Mono<UserPageResponse> listUsers(int page, int size, String keyword) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int offset = (normalizedPage - 1) * normalizedSize;

        Mono<Long> totalMono = userQueryRepository.count(keyword);
        Mono<List<UserView>> itemsMono = userQueryRepository.findPage(keyword, normalizedSize, offset)
                .map(userMapStructer::toView)
                .collectList();

        return Mono.zip(itemsMono, totalMono)
                .map(tuple -> new UserPageResponse(tuple.getT1(), normalizedPage, normalizedSize, tuple.getT2()));
    }

    @Override
    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(userRepository::delete);
    }

    @Override
    public Mono<UserView> resetPassword(Long id, String newPassword) {
        if (isBlank(newPassword)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能为空"));
        }
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(user);
                })
                .map(userMapStructer::toView);
    }

    @Override
    public Mono<UserView> updateUsername(Long id, UpdateUsernameRequest request) {
        if (request == null || isBlank(request.username())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空"));
        }
        String newUsername = request.username().trim();
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    if (newUsername.equals(user.getUsername())) {
                        return Mono.just(user);
                    }
                    return userRepository.existsByUsername(newUsername)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在"));
                                }
                                user.setUsername(newUsername);
                                user.setUpdatedAt(Instant.now().toEpochMilli());
                                return userRepository.save(user);
                            });
                })
                .map(userMapStructer::toView);
    }

    @Override
    public Mono<UserView> updatePassword(Long id, UpdatePasswordRequest request) {
        if (request == null || isBlank(request.oldPassword()) || isBlank(request.newPassword())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空"));
        }
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "旧密码错误"));
                    }
                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    user.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(user);
                })
                .map(userMapStructer::toView);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
