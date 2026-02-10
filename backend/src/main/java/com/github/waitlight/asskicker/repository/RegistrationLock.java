package com.github.waitlight.asskicker.repository;

import reactor.core.publisher.Mono;

public interface RegistrationLock {
    Mono<Void> acquire();
}
