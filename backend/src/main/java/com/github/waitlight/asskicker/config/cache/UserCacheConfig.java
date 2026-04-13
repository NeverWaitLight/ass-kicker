package com.github.waitlight.asskicker.config.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * UserService 专用的 Caffeine 缓存配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheConfig {

    private static final long MAXIMUM_SIZE = 1000;
    private static final long EXPIRE_AFTER_WRITE_MINUTES = 10;
    private static final int RANDOM_JITTER_PERCENT = 20;

    private final UserRepository userRepository;

    private AsyncLoadingCache<String, Optional<UserEntity>> userByIdCache;
    private AsyncLoadingCache<String, Optional<UserEntity>> userByUsernameCache;

    @PostConstruct
    public void init() {
        userByIdCache = buildCache((id, executor) -> userRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        userByUsernameCache = buildCache((username, executor) -> userRepository.findByUsername(username)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        log.info("UserCache initialized: maxSize={}, expireMinutes={}, jitterPercent={}",
                MAXIMUM_SIZE, EXPIRE_AFTER_WRITE_MINUTES, RANDOM_JITTER_PERCENT);
    }

    private <K, V> AsyncLoadingCache<K, V> buildCache(
            BiFunction<K, Executor, CompletableFuture<V>> loader) {
        return Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(jitteredExpireMinutes(), TimeUnit.MINUTES)
                .buildAsync(loader::apply);
    }

    /**
     * 计算带随机抖动的过期时间（分钟），用于防雪崩
     */
    private long jitteredExpireMinutes() {
        if (RANDOM_JITTER_PERCENT == 0) {
            return EXPIRE_AFTER_WRITE_MINUTES;
        }
        Random random = new Random();
        double jitterFactor = 1.0 + (random.nextDouble() * 2 - 1) * RANDOM_JITTER_PERCENT / 100.0;
        long jittered = Math.round(EXPIRE_AFTER_WRITE_MINUTES * jitterFactor);
        return Math.max(1, jittered);
    }

    public AsyncLoadingCache<String, Optional<UserEntity>> getUserByIdCache() {
        return userByIdCache;
    }

    public AsyncLoadingCache<String, Optional<UserEntity>> getUserByUsernameCache() {
        return userByUsernameCache;
    }
}