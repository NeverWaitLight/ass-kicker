package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.UserCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCacheConfig userCacheConfig;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private AsyncLoadingCache<String, Optional<UserEntity>> userByIdCache;
    private AsyncLoadingCache<String, Optional<UserEntity>> userByUsernameCache;

    @jakarta.annotation.PostConstruct
    void initCaches() {
        userByIdCache = userCacheConfig.getUserByIdCache();
        userByUsernameCache = userCacheConfig.getUserByUsernameCache();
    }

    public Mono<UserEntity> create(UserEntity user) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Mono.error(new BadRequestException("user.error.usernameOrPasswordEmpty"));
        }

        String username = user.getUsername().trim();
        return Mono.fromFuture(userByUsernameCache.get(username))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ConflictException("user.error.usernameExists")))
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
                    user.setDeletedAt(SoftDeleteConstants.NOT_DELETED);
                    return userRepository.save(user);
                });
    }

    public Mono<UserEntity> getById(String id) {
        return Mono.fromFuture(userByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("user.error.userNotFound", id))));
    }

    public Mono<Long> count(String keyword) {
        return userRepository.countByKeyword(keyword);
    }

    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.findPage(keyword, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.error.userNotFound", id)))
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
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(oldPassword)) {
            return Mono.error(new BadRequestException("user.error.passwordEmpty"));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.error.userNotFound", id)))
                .filter(user -> passwordEncoder.matches(oldPassword, user.getPassword()))
                .switchIfEmpty(Mono.error(new PermissionDeniedException("user.error.oldPasswordIncorrect")))
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
        if (u == null || !StringUtils.hasText(u.getId())) {
            return Mono.error(new BadRequestException("user.error.userIdEmpty"));
        }

        return userRepository.findById(u.getId())
                .switchIfEmpty(Mono.error(new NotFoundException("user.error.userNotFound", u.getId())))
                .flatMap(existing -> {
                    String id = u.getId();
                    String newUsername = u.getUsername();
                    UserStatus newStatus = u.getStatus();

                    if (newUsername == null || newUsername.isBlank()) {
                        return Mono.just(u);
                    }

                    String trimmedUsername = newUsername.trim();
                    if (trimmedUsername.equals(u.getUsername())) {
                        return Mono.just(u);
                    }

                    return Mono.fromFuture(userByUsernameCache.get(trimmedUsername))
                            .filter(Optional::isEmpty)
                            .switchIfEmpty(Mono.error(new ConflictException("user.error.usernameExists")))
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
                });
    }
}