package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.SendRecord;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SendRecordRepository extends ReactiveMongoRepository<SendRecord, String>, SendRecordRepositoryCustom {

    Flux<SendRecord> findByTaskId(String taskId);
}

interface SendRecordRepositoryCustom {

    Flux<SendRecord> findPage(int limit, int offset);

    Mono<Long> countAll();
}

class SendRecordRepositoryCustomImpl implements SendRecordRepositoryCustom {

    private final ReactiveMongoTemplate mongoTemplate;

    SendRecordRepositoryCustomImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<SendRecord> findPage(int limit, int offset) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "sentAt"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, SendRecord.class);
    }

    @Override
    public Mono<Long> countAll() {
        return mongoTemplate.count(new Query(), SendRecord.class);
    }
}
