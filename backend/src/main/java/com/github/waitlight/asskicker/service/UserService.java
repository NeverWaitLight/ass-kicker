package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
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
                .buildCache((username, executor) -> userRepository.findByUsername(username)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    /**
     * Creates a new user with the provided user entity data.
     */
    public Mono<UserEntity> create(UserEntity u) {
        if (u == null || !StringUtils.hasText(u.getUsername()) || !StringUtils.hasText(u.getPassword())) {
            return Mono.error(new BadRequestException("user.usernameOrPassword.empty"));
        }

        return userRepository.findByUsername(u.getUsername())
                .flatMap(existing -> Mono.<UserEntity>error(new ConflictException("user.username.exists")))
                .switchIfEmpty(Mono.defer(() -> userRepository.save(initNewUser(u))));
    }

    private UserEntity initNewUser(UserEntity u) {
        long now = Instant.now().toEpochMilli();
        u.setId(snowflakeIdGenerator.nextIdString());
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setRole(Optional.ofNullable(u.getRole()).orElse(UserRole.MEMBER));
        u.setStatus(Optional.ofNullable(u.getStatus()).orElse(UserStatus.ACTIVE));
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        u.setDeletedAt(SoftDeleteConstants.NOT_DELETED);
        return u;
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
        return userRepository.count(keyword);
    }

    /**
     * Retrieves a paginated list of users matching the given keyword.
     */
    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.list(keyword, limit, offset);
    }

    /**
     * Soft deletes a user by marking it as deleted.
     */
    public Mono<Void> delete(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .flatMap(user -> userRepository.save(markAsDeleted(user))
                        .doOnSuccess(saved -> invalidateUserCaches(user, saved)))
                .then();
    }

    private UserEntity markAsDeleted(UserEntity user) {
        long now = Instant.now().toEpochMilli();
        user.setDeletedAt(now);
        user.setUpdatedAt(now);
        return user;
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
                .flatMap(user -> userRepository.save(applyNewPassword(user, newPassword))
                        .doOnSuccess(saved -> invalidateUserCaches(user, saved)));
    }

    private UserEntity applyNewPassword(UserEntity user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now().toEpochMilli());
        return user;
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
                .flatMap(existing -> ensureUsernameAvailable(u, existing)
                        .then(Mono.defer(() -> {
                            u.setUpdatedAt(Instant.now().toEpochMilli());
                            return userRepository.save(u)
                                    .doOnSuccess(saved -> invalidateUserCaches(existing, saved));
                        })));
    }

    private Mono<Void> ensureUsernameAvailable(UserEntity u, UserEntity existing) {
        String newUsername = u.getUsername();
        if (!StringUtils.hasText(newUsername) || newUsername.trim().equals(existing.getUsername())) {
            return Mono.empty();
        }
        u.setUsername(newUsername.trim());
        return userRepository.findByUsername(u.getUsername())
                .filter(found -> !found.getId().equals(u.getId()))
                .flatMap(found -> Mono.<Void>error(new ConflictException("user.username.exists")));
    }

    private void invalidateUserCaches(UserEntity existing, UserEntity saved) {
        userByIdCache.synchronous().invalidate(saved.getId());
        userByUsernameCache.synchronous().invalidate(existing.getUsername());
        userByUsernameCache.synchronous().invalidate(saved.getUsername());
    }
}