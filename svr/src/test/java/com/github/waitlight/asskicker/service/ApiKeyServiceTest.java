package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.model.ApiKeyEntity;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CaffeineCacheConfig caffeineCacheConfig;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        // 创建一个简单的 mock cache，避免 NPE
        AsyncLoadingCache<String, Object> mockCache = Caffeine.newBuilder()
                .buildAsync((key, executor) -> CompletableFuture.completedFuture(null));

        when(caffeineCacheConfig.buildCache(any())).thenAnswer(invocation -> mockCache);

        apiKeyService = new ApiKeyService(apiKeyRepository, passwordEncoder, caffeineCacheConfig);
        apiKeyService.init();
    }

    @Test
    void delete_fails_when_api_key_not_found() {
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.empty());

        StepVerifier.create(apiKeyService.delete("u_1", "ak_1"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(NotFoundException.class);
                })
                .verify();

        verify(apiKeyRepository, never()).deleteById(any(String.class));
    }

    @Test
    void delete_fails_when_user_has_no_permission() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("owner");
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));

        StepVerifier.create(apiKeyService.delete("other_user", "ak_1"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(PermissionDeniedException.class);
                })
                .verify();

        verify(apiKeyRepository, never()).deleteById(any(String.class));
    }

    @Test
    void delete_success_removes_entity() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("u_1");
        entity.setKeyPrefix("ak_123456789");
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));
        when(apiKeyRepository.deleteById("ak_1")).thenReturn(Mono.empty());

        StepVerifier.create(apiKeyService.delete("u_1", "ak_1"))
                .verifyComplete();

        verify(apiKeyRepository).deleteById("ak_1");
    }

    @Test
    void update_fails_when_api_key_not_found() {
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.empty());

        StepVerifier.create(apiKeyService.update("u_1", "ak_1", "new name"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(NotFoundException.class);
                })
                .verify();
    }

    @Test
    void update_fails_when_user_has_no_permission() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("owner");
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));

        StepVerifier.create(apiKeyService.update("other_user", "ak_1", "new name"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(PermissionDeniedException.class);
                })
                .verify();

        verify(apiKeyRepository, never()).save(any(ApiKeyEntity.class));
    }

    @Test
    void update_success_changes_name() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("u_1");
        entity.setName("old name");
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(apiKeyService.update("u_1", "ak_1", "new name"))
                .assertNext(saved -> assertThat(saved.getName()).isEqualTo("new name"))
                .verifyComplete();
    }
}