package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageTemplateRepository extends ReactiveMongoRepository<MessageTemplateEntity, String> {

    Mono<MessageTemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType);

    Flux<MessageTemplateEntity> findByChannelType(ChannelType channelType);
}
