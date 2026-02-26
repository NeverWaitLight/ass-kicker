package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Channel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRepository extends ReactiveMongoRepository<Channel, String> {
}
