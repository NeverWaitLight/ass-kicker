package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Channel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRepository extends ReactiveCrudRepository<Channel, Long> {
}