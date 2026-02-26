package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class UserQueryRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public UserQueryRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Flux<User> findPage(String keyword, int limit, int offset) {
        Query query = buildQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Long> count(String keyword) {
        Query query = buildQuery(keyword);
        return mongoTemplate.count(query, User.class);
    }

    private Query buildQuery(String keyword) {
        Query query = new Query();
        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(Criteria.where("username").regex(".*" + escapeRegex(keyword) + ".*", "i"));
        }
        return query;
    }

    private String escapeRegex(String input) {
        return input.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");
    }
}
