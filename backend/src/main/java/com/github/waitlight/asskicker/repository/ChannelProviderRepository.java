package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
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
public class ChannelProviderRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<ChannelProviderEntity> save(ChannelProviderEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<ChannelProviderEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, ChannelProviderEntity.class);
    }

    public Mono<ChannelProviderEntity> findByCode(String code) {
        Query query = new Query();
        query.addCriteria(Criteria.where("code").is(code));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, ChannelProviderEntity.class);
    }

    public Flux<ChannelProviderEntity> findByChannelType(ChannelType channelType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_type").is(channelType));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.find(query, ChannelProviderEntity.class);
    }

    public Flux<ChannelProviderEntity> findByEnabled(boolean enabled) {
        Query query = new Query();
        query.addCriteria(Criteria.where("enabled").is(enabled));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.find(query, ChannelProviderEntity.class);
    }

    public Flux<ChannelProviderEntity> findByChannelTypeAndEnabled(ChannelType channelType, boolean enabled) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channel_type").is(channelType));
        query.addCriteria(Criteria.where("enabled").is(enabled));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.find(query, ChannelProviderEntity.class);
    }

    public Flux<ChannelProviderEntity> findAll() {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        return mongoTemplate.find(query, ChannelProviderEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, ChannelProviderEntity.class).then();
    }

    public Flux<ChannelProviderEntity> list(String keyword, int limit, int offset) {
        Query query = buildKeywordQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, ChannelProviderEntity.class);
    }

    public Mono<Long> count(String keyword) {
        Query query = buildKeywordQuery(keyword);
        return mongoTemplate.count(query, ChannelProviderEntity.class);
    }

    private Query buildKeywordQuery(String keyword) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        if (StringUtils.hasText(keyword)) {
            query.addCriteria(Criteria.where("name").regex(".*" + keyword + ".*", "i"));
        }
        return query;
    }
}
