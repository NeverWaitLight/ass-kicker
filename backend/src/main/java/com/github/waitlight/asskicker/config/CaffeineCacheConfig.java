package com.github.waitlight.asskicker.config;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CaffeineCacheProperties.class)
public class CaffeineCacheConfig {

    private final CaffeineCacheProperties properties;
    private final Random random = new Random();

    public CaffeineCacheConfig(CaffeineCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * 计算带随机抖动的过期时间（分钟），用于防雪崩
     */
    public long jitteredExpireMinutes() {
        long base = properties.getExpireAfterWriteMinutes();
        int jitterPercent = properties.getRandomJitterPercent();
        if (jitterPercent == 0) {
            return base;
        }
        double jitterFactor = 1.0 + (random.nextDouble() * 2 - 1) * jitterPercent / 100.0;
        long jittered = Math.round(base * jitterFactor);
        return Math.max(1, jittered);
    }

    /**
     * 创建带雪崩防护的 AsyncLoadingCache
     */
    public <K, V> AsyncLoadingCache<K, V> buildCache(
            BiFunction<K, java.util.concurrent.Executor, java.util.concurrent.CompletableFuture<V>> loader) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .expireAfterWrite(jitteredExpireMinutes(), TimeUnit.MINUTES)
                .buildAsync(loader::apply);
    }
}
