package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final long NOT_DELETED = 0L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaffeineCacheConfig caffeineCacheConfig;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private AsyncLoadingCache<String, Optional<UserEntity>> userByIdCache;
    private AsyncLoadingCache<String, Optional<UserEntity>> userByUsernameCache;

    @PostConstruct
    void initCaches() {
        userByIdCache = caffeineCacheConfig.buildCache((id, executor) -> userRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        userByUsernameCache = caffeineCacheConfig
                .buildCache((username, executor) -> userRepository.findActiveByUsername(username)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    public Mono<UserEntity> create(UserEntity user) {
        if (user == null || isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }

        String username = user.getUsername().trim();
        return Mono.fromFuture(userByUsernameCache.get(username))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在")))
                .flatMap(opt -> {
                    long now = Instant.now().toEpochMilli();
                    user.setId(snowflakeIdGenerator.nextIdString());
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    if (user.getRole() == null) {
                        user.setRole(UserRole.MEMBER);
                    }
                    if (user.getStatus() == null) {
                        user.setStatus(UserStatus.ACTIVE);
                    }
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    user.setDeletedAt(NOT_DELETED);
                    return userRepository.save(user);
                });
    }

    public Mono<UserEntity> getById(String id) {
        return Mono.fromFuture(userByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"))));
    }

    public Mono<Long> count(String keyword) {
        return userRepository.countByKeyword(keyword);
    }

    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.findPage(keyword, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    long now = Instant.now().toEpochMilli();
                    user.setDeletedAt(now);
                    user.setUpdatedAt(now);
                    return userRepository.save(user);
                })
                .flatMap(saved -> {
                    userByIdCache.synchronous().invalidate(id);
                    userByUsernameCache.synchronous().invalidate(saved.getUsername());
                    return Mono.empty();
                });
    }

    public Mono<UserEntity> resetPassword(String id, String newPassword, String oldPassword) {
        if (isBlank(newPassword) || isBlank(oldPassword)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空"));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .filter(user -> passwordEncoder.matches(oldPassword, user.getPassword()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "旧密码错误")))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(user)
                            .doOnSuccess(saved -> {
                                userByIdCache.synchronous().invalidate(id);
                                userByUsernameCache.synchronous().invalidate(saved.getUsername());
                            });
                });
    }

    public Mono<UserEntity> update(UserEntity u) {
        if (u == null || isBlank(u.getId())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户ID不能为空"));
        }

        return userRepository.findById(u.getId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(this::doUpdate);
    }

    private Mono<UserEntity> doUpdate(UserEntity u) {
        String id = u.getId();
        String newUsername = u.getUsername();
        UserStatus newStatus = u.getStatus();

        if (newUsername == null || newUsername.isBlank()) {
            return updateStatusOnly(u);
        }

        String trimmedUsername = newUsername.trim();
        if (trimmedUsername.equals(u.getUsername())) {
            return updateStatusOnly(u);
        }

        return Mono.fromFuture(userByUsernameCache.get(trimmedUsername))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在")))
                .flatMap(opt -> {
                    String oldUsername = u.getUsername();
                    u.setUsername(trimmedUsername);
                    if (newStatus != null) {
                        u.setStatus(newStatus);
                    }
                    u.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(u)
                            .doOnSuccess(saved -> {
                                userByIdCache.synchronous().invalidate(id);
                                userByUsernameCache.synchronous().invalidate(oldUsername);
                                userByUsernameCache.synchronous().invalidate(trimmedUsername);
                            });
                });
    }

    private Mono<UserEntity> updateStatusOnly(UserEntity user) {
        return Mono.just(user);

    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}