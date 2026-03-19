package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelConfig;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelConfigRepository extends ReactiveMongoRepository<ChannelConfig, String> {
}
