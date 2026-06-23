package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ChannelRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<ChannelEntity> save(ChannelEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<ChannelEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, ChannelEntity.class);
    }

    public Mono<ChannelEntity> findByCode(String code) {
        Query query = new Query();
        query.addCriteria(Criteria.where("code").is(code));
        return mongoTemplate.findOne(query, ChannelEntity.class);
    }

    public Flux<ChannelEntity> findByChannelType(ChannelType channelType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_type").is(channelType));
        return mongoTemplate.find(query, ChannelEntity.class);
    }

    public Flux<ChannelEntity> findByEnabled(boolean enabled) {
        Query query = new Query();
        query.addCriteria(Criteria.where("enabled").is(enabled));
        return mongoTemplate.find(query, ChannelEntity.class);
    }

    public Flux<ChannelEntity> findByChannelTypeAndEnabled(ChannelType channelType, boolean enabled) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_type").is(channelType));
        query.addCriteria(Criteria.where("enabled").is(enabled));
        return mongoTemplate.find(query, ChannelEntity.class);
    }

    public Flux<ChannelEntity> findAll() {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        return mongoTemplate.find(query, ChannelEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, ChannelEntity.class).then();
    }

    public Mono<Void> deleteAll() {
        Query query = new Query();
        return mongoTemplate.remove(query, ChannelEntity.class).then();
    }

    public Flux<ChannelEntity> saveAll(Flux<ChannelEntity> entities) {
        return entities.flatMap(mongoTemplate::save);
    }

    public Flux<ChannelEntity> list(String keyword, ChannelType channelType, ProviderType providerType, int limit,
            int offset) {
        Query query = buildQuery(keyword, channelType, providerType);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, ChannelEntity.class);
    }

    public Mono<Long> count(String keyword, ChannelType channelType, ProviderType providerType) {
        Query query = buildQuery(keyword, channelType, providerType);
        return mongoTemplate.count(query, ChannelEntity.class);
    }

    private Query buildQuery(String keyword, ChannelType channelType, ProviderType providerType) {
        Query query = new Query();
        if (StringUtils.hasText(keyword)) {
            query.addCriteria(Criteria.where("name").regex(".*" + keyword + ".*", "i"));
        }
        if (channelType != null) {
            query.addCriteria(Criteria.where("channel_type").is(channelType));
        }
        if (providerType != null) {
            query.addCriteria(Criteria.where("provider_type").is(providerType));
        }
        return query;
    }
}