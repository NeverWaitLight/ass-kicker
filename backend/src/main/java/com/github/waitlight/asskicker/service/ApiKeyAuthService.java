package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.security.UserPrincipal;
import reactor.core.publisher.Mono;

public interface ApiKeyAuthService {

    Mono<UserPrincipal> authenticate(String rawKey);

    void invalidateCache(String keyPrefix);
}
