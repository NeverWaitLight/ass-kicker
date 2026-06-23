package com.github.waitlight.asskicker.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.github.waitlight.asskicker.model.GlobalVariableEntity;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GlobalVariableRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<GlobalVariableEntity> save(GlobalVariableEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<GlobalVariableEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, GlobalVariableEntity.class);
    }

    public Mono<GlobalVariableEntity> findByKey(String key) {
        Query query = new Query();
        query.addCriteria(Criteria.where("key").is(key));
        return mongoTemplate.findOne(query, GlobalVariableEntity.class);
    }

    public Flux<GlobalVariableEntity> findEnabled() {
        Query query = new Query();
        query.addCriteria(Criteria.where("enabled").is(true));
        query.with(Sort.by(Sort.Direction.ASC, "key"));
        return mongoTemplate.find(query, GlobalVariableEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, GlobalVariableEntity.class).then();
    }

    public Flux<GlobalVariableEntity> list(String keyword, int limit, int offset) {
        Query query = buildListQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, GlobalVariableEntity.class);
    }

    public Mono<Long> count(String keyword) {
        return mongoTemplate.count(buildListQuery(keyword), GlobalVariableEntity.class);
    }

    public Mono<Void> deleteAll() {
        return mongoTemplate.remove(new Query(), GlobalVariableEntity.class).then();
    }

    private Query buildListQuery(String keyword) {
        Query query = new Query();
        if (StringUtils.hasText(keyword)) {
            String pattern = ".*" + keyword + ".*";
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("key").regex(pattern, "i"),
                    Criteria.where("name").regex(pattern, "i")));
        }
        return query;
    }
}
