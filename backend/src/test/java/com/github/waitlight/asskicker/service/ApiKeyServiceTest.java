package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.ApiKeyEntity;
import com.github.waitlight.asskicker.model.ApiKeyStatus;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    private ApiKeyAuthService apiKeyAuthService;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    void revoke_fails_when_api_key_not_found() {
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.empty());

        StepVerifier.create(apiKeyService.revoke("u_1", "ak_1"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("API Key 不存在");
                })
                .verify();

        verify(apiKeyRepository, never()).save(any(ApiKeyEntity.class));
    }

    @Test
    void revoke_fails_when_user_has_no_permission() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("owner");
        entity.setStatus(ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));

        StepVerifier.create(apiKeyService.revoke("other_user", "ak_1"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(403);
                    assertThat(rse.getReason()).isEqualTo("无权操作");
                })
                .verify();

        verify(apiKeyRepository, never()).save(any(ApiKeyEntity.class));
        verify(apiKeyAuthService, never()).invalidateCache(any(String.class));
    }

    @Test
    void revoke_fails_when_api_key_already_revoked() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("u_1");
        entity.setStatus(ApiKeyStatus.REVOKED);
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));

        StepVerifier.create(apiKeyService.revoke("u_1", "ak_1"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                    assertThat(rse.getReason()).isEqualTo("API Key 已销毁");
                })
                .verify();

        verify(apiKeyRepository, never()).save(any(ApiKeyEntity.class));
        verify(apiKeyAuthService, never()).invalidateCache(any(String.class));
    }

    @Test
    void revoke_success_updates_status_and_invalidates_cache() {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId("ak_1");
        entity.setUserId("u_1");
        entity.setStatus(ApiKeyStatus.ACTIVE);
        entity.setKeyPrefix("ak_123456789");
        when(apiKeyRepository.findById("ak_1")).thenReturn(Mono.just(entity));
        when(apiKeyRepository.save(any(ApiKeyEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(apiKeyService.revoke("u_1", "ak_1"))
                .verifyComplete();

        assertThat(entity.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(entity.getRevokedAt()).isNotNull();
        verify(apiKeyAuthService).invalidateCache("ak_123456789");
    }
}
