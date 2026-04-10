package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<UserEntity> save(UserEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<UserEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, UserEntity.class);
    }

    public Mono<UserEntity> findActiveByUsername(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(username));
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        return mongoTemplate.findOne(query, UserEntity.class);
    }

    public Flux<UserEntity> findPage(String keyword, int limit, int offset) {
        Query query = buildKeywordQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, UserEntity.class);
    }

    public Mono<Long> countByKeyword(String keyword) {
        Query query = buildKeywordQuery(keyword);
        return mongoTemplate.count(query, UserEntity.class);
    }

    private Query buildKeywordQuery(String keyword) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted_at").is(SoftDeleteConstants.NOT_DELETED));
        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(Criteria.where("username").regex(".*" + escapeRegex(keyword) + ".*", "i"));
        }
        return query;
    }

    private String escapeRegex(String input) {
        return input.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");
    }

}
