package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}
