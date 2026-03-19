package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelEntityRepository extends ReactiveMongoRepository<ChannelEntity, String> {
}
