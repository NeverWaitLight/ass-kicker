package com.github.waitlight.asskicker.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostgresRegistrationLock implements RegistrationLock {

    private static final long REGISTRATION_LOCK_KEY = 912_867_235L;

    private final DatabaseClient databaseClient;

    public PostgresRegistrationLock(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> acquire() {
        return databaseClient.sql("SELECT pg_advisory_xact_lock(:lockKey)")
                .bind("lockKey", REGISTRATION_LOCK_KEY)
                .fetch()
                .rowsUpdated()
                .then();
    }
}
