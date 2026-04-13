package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.SendRecordEntity;
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
public class SendRecordRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<SendRecordEntity> save(SendRecordEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<SendRecordEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, SendRecordEntity.class);
    }

    public Flux<SendRecordEntity> findByTaskId(String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        return mongoTemplate.find(query, SendRecordEntity.class);
    }

    public Flux<SendRecordEntity> findPage(int limit, int offset, String recipient, String channelType) {
        Query query = buildListQuery(recipient, channelType);
        query.with(Sort.by(Sort.Direction.DESC, "sentAt"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, SendRecordEntity.class);
    }

    public Mono<Long> countAll(String recipient, String channelType) {
        return mongoTemplate.count(buildListQuery(recipient, channelType), SendRecordEntity.class);
    }

    private Query buildListQuery(String recipient, String channelType) {
        Query query = new Query();
        if (recipient != null && !recipient.isBlank()) {
            String term = recipient.trim();
            query.addCriteria(Criteria.where("recipient").is(term));
        }
        if (channelType != null && !channelType.isBlank()) {
            query.addCriteria(Criteria.where("channel_type").is(channelType.trim()));
        }
        return query;
    }
}
