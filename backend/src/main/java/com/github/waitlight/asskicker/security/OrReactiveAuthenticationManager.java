package com.github.waitlight.asskicker.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * 先尝试 JWT Manager，失败（token 形似 API Key）则委托给 API Key Manager。
 * 通过 token 是否含点来区分类型，避免不必要的 BCrypt 开销。
 */
public class OrReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtReactiveAuthenticationManager jwtManager;
    private final ApiKeyReactiveAuthenticationManager apiKeyManager;

    public OrReactiveAuthenticationManager(JwtReactiveAuthenticationManager jwtManager,
                                           ApiKeyReactiveAuthenticationManager apiKeyManager) {
        this.jwtManager = jwtManager;
        this.apiKeyManager = apiKeyManager;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String credentials = authentication.getCredentials() == null
                ? null : authentication.getCredentials().toString();

        if (credentials != null && looksLikeJwt(credentials)) {
            return jwtManager.authenticate(authentication);
        }
        return apiKeyManager.authenticate(authentication);
    }

    private boolean looksLikeJwt(String token) {
        int first = token.indexOf('.');
        if (first < 0) return false;
        int second = token.indexOf('.', first + 1);
        return second > first;
    }
}
