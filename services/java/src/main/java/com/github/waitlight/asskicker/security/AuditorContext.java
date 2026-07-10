package com.github.waitlight.asskicker.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 从 Reactor Context 中拉取当前登录用户的 ID，供审计切面使用
 */
public final class AuditorContext {

    private AuditorContext() {
    }

    public static Mono<Optional<String>> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> auth.getPrincipal() instanceof UserPrincipal p ? Optional.ofNullable(p.userId())
                        : Optional.<String>empty())
                .defaultIfEmpty(Optional.empty());
    }
}
