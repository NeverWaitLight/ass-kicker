package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.service.ApiKeyAuthService;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

public class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ApiKeyAuthService apiKeyAuthService;

    public ApiKeyReactiveAuthenticationManager(ApiKeyAuthService apiKeyAuthService) {
        this.apiKeyAuthService = apiKeyAuthService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String rawKey = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        if (rawKey == null || rawKey.isBlank()) {
            return Mono.empty();
        }
        return apiKeyAuthService.authenticate(rawKey)
                .map(principal -> {
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                    return (Authentication) new UsernamePasswordAuthenticationToken(principal, rawKey, authorities);
                });
    }
}
