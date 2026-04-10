package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.RegisterDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private static final long NOT_DELETED = 0L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userMapStructer;
    private final CaffeineCacheConfig caffeineCacheConfig;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private AsyncLoadingCache<String, Optional<UserEntity>> userByIdCache;
    private AsyncLoadingCache<String, Optional<UserEntity>> userByUsernameCache;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            UserConverter userMapStructer, CaffeineCacheConfig caffeineCacheConfig,
            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapStructer = userMapStructer;
        this.caffeineCacheConfig = caffeineCacheConfig;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @PostConstruct
    void initCaches() {
        userByIdCache = caffeineCacheConfig.buildCache((id, executor) -> userRepository.findActiveById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        userByUsernameCache = caffeineCacheConfig
                .buildCache((username, executor) -> userRepository.findActiveByUsername(username)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    public Mono<UserVO> create(UserEntity user) {
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
                    return userRepository.save(user).map(userMapStructer::toView);
                });
    }

    public Mono<UserVO> register(RegisterDTO request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }

        String username = request.username().trim();
        return Mono.fromFuture(userByUsernameCache.get(username))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在")))
                .flatMap(opt -> userRepository.countByKeyword(null))
                .flatMap(count -> {
                    UserEntity user = new UserEntity();
                    long now = Instant.now().toEpochMilli();
                    user.setId(snowflakeIdGenerator.nextIdString());
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(request.password()));
                    user.setRole(count == 0 ? UserRole.ADMIN : UserRole.MEMBER);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    user.setDeletedAt(NOT_DELETED);
                    return userRepository.save(user).map(userMapStructer::toView);
                });
    }

    public Mono<UserVO> getById(String id) {
        return Mono.fromFuture(userByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(u -> Mono.just(userMapStructer.toView(u)))
                        .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"))));
    }

    public Mono<Long> count(String keyword) {
        return userRepository.countByKeyword(keyword);
    }

    public Flux<UserEntity> list(String keyword, int limit, int offset) {
        return userRepository.findPage(keyword, limit, offset);
    }

    public Mono<Void> delete(String id) {
        return userRepository.findActiveById(id)
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

    public Mono<UserVO> resetPassword(String id, String newPassword, String oldPassword) {
        if (isBlank(newPassword) || isBlank(oldPassword)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空"));
        }

        return userRepository.findActiveById(id)
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
                })
                .map(userMapStructer::toView);
    }

    public Mono<UserVO> update(UserEntity request) {
        if (request == null || isBlank(request.getId())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户ID不能为空"));
        }

        String id = request.getId();
        String newUsername = request.getUsername();
        UserStatus newStatus = request.getStatus();

        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")))
                .flatMap(user -> updateUser(user, id, newUsername, newStatus))
                .map(userMapStructer::toView);
    }

    private Mono<UserEntity> updateUser(UserEntity user, String id, String newUsername, UserStatus newStatus) {
        if (newUsername == null || newUsername.isBlank()) {
            return updateStatusOnly(user, id, newStatus);
        }

        String trimmedUsername = newUsername.trim();
        if (trimmedUsername.equals(user.getUsername())) {
            return updateStatusOnly(user, id, newStatus);
        }

        return Mono.fromFuture(userByUsernameCache.get(trimmedUsername))
                .filter(Optional::isEmpty)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在")))
                .flatMap(opt -> {
                    String oldUsername = user.getUsername();
                    user.setUsername(trimmedUsername);
                    if (newStatus != null) {
                        user.setStatus(newStatus);
                    }
                    user.setUpdatedAt(Instant.now().toEpochMilli());
                    return userRepository.save(user)
                            .doOnSuccess(saved -> {
                                userByIdCache.synchronous().invalidate(id);
                                userByUsernameCache.synchronous().invalidate(oldUsername);
                                userByUsernameCache.synchronous().invalidate(trimmedUsername);
                            });
                });
    }

    private Mono<UserEntity> updateStatusOnly(UserEntity user, String id, UserStatus newStatus) {
        if (newStatus == null || newStatus == user.getStatus()) {
            return Mono.just(user);
        }

        user.setStatus(newStatus);
        user.setUpdatedAt(Instant.now().toEpochMilli());
        return userRepository.save(user)
                .doOnSuccess(saved -> {
                    userByIdCache.synchronous().invalidate(id);
                    userByUsernameCache.synchronous().invalidate(saved.getUsername());
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}