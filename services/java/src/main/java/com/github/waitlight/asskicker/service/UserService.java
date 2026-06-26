package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
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
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setRole(Optional.ofNullable(u.getRole()).orElse(UserRole.DEVELOPER));
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        u.setDeletedAt(SoftDeleteConstants.NOT_DELETED);
        return u;
    }

    public Mono<UserEntity> getById(String id) {
        return Mono.fromFuture(userByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("user.id.notFound", id))));
    }

    public Mono<UserEntity> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Mono.empty();
        }
        String key = username.trim();
        return Mono.fromFuture(userByUsernameCache.get(key))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<UserEntity> recordLogin(UserEntity user) {
        long now = Instant.now().toEpochMilli();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user)
                .doOnSuccess(saved -> invalidateUserCaches(user, saved));
    }

    public Mono<Long> count(String keyword) {
        return userRepository.count(keyword);
    }

    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.list(keyword, limit, offset);
    }

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
        user.setKickedOutAt(now);
        user.setUpdatedAt(now);
        return user;
    }

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

    public Mono<UserEntity> resetPassword(String id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            return Mono.error(new BadRequestException("user.password.empty"));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .flatMap(user -> userRepository.save(applyNewPassword(user, newPassword))
                        .doOnSuccess(saved -> invalidateUserCaches(user, saved)));
    }

    private UserEntity applyNewPassword(UserEntity user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now().toEpochMilli());
        return user;
    }

    public Mono<UserEntity> updateStatus(String id, UserStatus status) {
        if (status == null) {
            return Mono.error(new BadRequestException("user.status.empty"));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .flatMap(existing -> {
                    if (existing.getStatus() == status) {
                        return Mono.just(existing);
                    }
                    existing.setStatus(status);
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(existing)
                            .doOnSuccess(saved -> invalidateUserCaches(existing, saved));
                });
    }

    public Mono<UserEntity> updateUsername(String id, String newUsername) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(newUsername)) {
            return Mono.error(new BadRequestException("user.id.or.username.empty"));
        }

        String trimmedUsername = newUsername.trim();
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .flatMap(existing -> {
                    if (trimmedUsername.equals(existing.getUsername())) {
                        return Mono.just(existing);
                    }
                    return userRepository.findByUsername(trimmedUsername)
                            .flatMap(found -> Mono.<UserEntity>error(new ConflictException("user.username.exists")))
                            .switchIfEmpty(Mono.defer(() -> {
                                existing.setUsername(trimmedUsername);
                                existing.setUpdatedAt(Instant.now().toEpochMilli());
                                return userRepository.save(existing)
                                        .doOnSuccess(saved -> invalidateUserCaches(existing, saved));
                            }));
                });
    }

    public Mono<UserEntity> kickOut(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("user.id.notFound", id)))
                .flatMap(user -> {
                    long now = Instant.now().toEpochMilli();
                    user.setKickedOutAt(now);
                    user.setUpdatedAt(now);
                    return userRepository.save(user)
                            .doOnSuccess(saved -> invalidateUserCaches(user, saved));
                });
    }

    private void invalidateUserCaches(UserEntity existing, UserEntity saved) {
        userByIdCache.synchronous().invalidate(saved.getId());
        userByUsernameCache.synchronous().invalidate(existing.getUsername());
        userByUsernameCache.synchronous().invalidate(saved.getUsername());
    }
}