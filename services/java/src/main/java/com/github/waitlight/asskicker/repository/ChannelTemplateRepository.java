package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ChannelTemplateRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<ChannelTemplateEntity> save(ChannelTemplateEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<ChannelTemplateEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, ChannelTemplateEntity.class);
    }

    public Mono<ChannelTemplateEntity> findByLocalizedTemplateIdAndChannelId(String localizedTemplateId,
            String channelId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localized_template_id").is(localizedTemplateId));
        query.addCriteria(Criteria.where("channel_id").is(channelId));
        return mongoTemplate.findOne(query, ChannelTemplateEntity.class);
    }

    public Flux<ChannelTemplateEntity> findByLocalizedTemplateId(String localizedTemplateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localized_template_id").is(localizedTemplateId));
        return mongoTemplate.find(query, ChannelTemplateEntity.class);
    }

    public Flux<ChannelTemplateEntity> findByChannelId(String channelId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_id").is(channelId));
        return mongoTemplate.find(query, ChannelTemplateEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, ChannelTemplateEntity.class).then();
    }

    public Mono<Void> deleteByLocalizedTemplateId(String localizedTemplateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localized_template_id").is(localizedTemplateId));
        return mongoTemplate.remove(query, ChannelTemplateEntity.class).then();
    }

    public Mono<Void> deleteByChannelId(String channelId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_id").is(channelId));
        return mongoTemplate.remove(query, ChannelTemplateEntity.class).then();
    }
}
