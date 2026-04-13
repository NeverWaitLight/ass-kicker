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
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    void initCaches() {
        userByIdCache = userCacheConfig.getUserByIdCache();
        userByUsernameCache = userCacheConfig.getUserByUsernameCache();
    }

    /**
     * Creates a new user with the provided user entity data.
     */
    public Mono<UserEntity> create(UserEntity u) {
        if (u == null || !StringUtils.hasText(u.getUsername()) || !StringUtils.hasText(u.getPassword())) {
            return Mono.error(new BadRequestException("user.usernameOrPassword.empty"));
        }

        String username = u.getUsername().trim();
        return Mono.fromFuture(userByUsernameCache.get(username))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ConflictException("user.username.exists")))
                .flatMap(opt -> {
                    long now = Instant.now().toEpochMilli();
                    u.setId(snowflakeIdGenerator.nextIdString());
                    u.setUsername(username);
                    u.setPassword(passwordEncoder.encode(u.getPassword()));
                    if (u.getRole() == null) {
                        u.setRole(UserRole.MEMBER);
                    }
                    if (u.getStatus() == null) {
                        u.setStatus(UserStatus.ACTIVE);
                    }
                    u.setCreatedAt(now);
                    u.setUpdatedAt(now);
                    u.setDeletedAt(SoftDeleteConstants.NOT_DELETED);
                    return userRepository.save(u);
                });
    }

    /**
     * Retrieves a user entity by its unique identifier.
     */
    public Mono<UserEntity> getById(String id) {
        return Mono.fromFuture(userByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("user.id.notFound", id))));
    }

    /**
     * Counts the total number of users matching the given keyword.
     */
    public Mono<Long> count(String keyword) {
        return userRepository.countByKeyword(keyword);
    }

    /**
     * Retrieves a paginated list of users matching the given keyword.
     */
    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.findPage(keyword, limit, offset);
    }

    /**
     * Soft deletes a user by marking it as deleted.
     */
    public Mono<Void> delete(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
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

    /**
     * Resets a user's password after validating the old password.
     */
    public Mono<UserEntity> resetPassword(String id, String newPassword, String oldPassword) {
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(oldPassword)) {
            return Mono.error(new BadRequestException("user.password.empty"));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .filter(user -> passwordEncoder.matches(oldPassword, user.getPassword()))
                .switchIfEmpty(Mono.error(new PermissionDeniedException("user.oldPassword.incorrect")))
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

    /**
     * Updates an existing user's information.
     */
    public Mono<UserEntity> update(UserEntity u) {
        if (u == null || !StringUtils.hasText(u.getId())) {
            return Mono.error(new BadRequestException("user.id.empty"));
        }

        return userRepository.findById(u.getId())
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", u.getId())))
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
                            .switchIfEmpty(Mono.error(new ConflictException("user.username.exists")))
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