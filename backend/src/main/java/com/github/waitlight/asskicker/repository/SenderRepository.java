package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Sender;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SenderRepository extends ReactiveMongoRepository<Sender, String> {
}
