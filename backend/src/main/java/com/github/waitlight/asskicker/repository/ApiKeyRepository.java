package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ApiKeyEntity;
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
public class ApiKeyRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<ApiKeyEntity> save(ApiKeyEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<ApiKeyEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, ApiKeyEntity.class);
    }

    public Mono<ApiKeyEntity> findByKeyPrefix(String keyPrefix) {
        Query query = new Query();
        query.addCriteria(Criteria.where("key_prefix").is(keyPrefix));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, ApiKeyEntity.class);
    }

    public Flux<ApiKeyEntity> findByUserId(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("user_id").is(userId));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.find(query, ApiKeyEntity.class);
    }

    public Flux<ApiKeyEntity> list(String keyword, int limit, int offset) {
        Query query = buildKeywordQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, ApiKeyEntity.class);
    }

    public Mono<Long> count(String keyword) {
        Query query = buildKeywordQuery(keyword);
        return mongoTemplate.count(query, ApiKeyEntity.class);
    }

    private Query buildKeywordQuery(String keyword) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        if (StringUtils.hasText(keyword)) {
            query.addCriteria(Criteria.where("key_prefix").regex(".*" + keyword + ".*", "i"));
        }
        return query;
    }
}
