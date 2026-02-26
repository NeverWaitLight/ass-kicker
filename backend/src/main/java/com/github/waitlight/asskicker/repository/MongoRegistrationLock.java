package com.github.waitlight.asskicker.repository;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class MongoRegistrationLock implements RegistrationLock {

    private static final String LOCK_COLLECTION = "registration_lock";
    private static final String LOCK_ID = "registration";

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoRegistrationLock(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> acquire() {
        Query query = new Query(Criteria.where("_id").is(LOCK_ID));
        Update update = new Update()
                .set("locked_at", Instant.now().toEpochMilli());
        return mongoTemplate.upsert(query, update, LOCK_COLLECTION).then();
    }
}
