package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.RecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RecordRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<RecordEntity> save(RecordEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<Void> saveAll(List<RecordEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(entities).concatMap(mongoTemplate::save).then();
    }

    public Mono<RecordEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, RecordEntity.class);
    }

    public Flux<RecordEntity> findByTaskId(String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        return mongoTemplate.find(query, RecordEntity.class);
    }

    public Flux<RecordEntity> findPage(int limit, int offset, String recipient, String channelType) {
        Query query = buildListQuery(recipient, channelType);
        query.with(Sort.by(Sort.Direction.DESC, "sentAt"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, RecordEntity.class);
    }

    public Mono<Long> countAll(String recipient, String channelType) {
        return mongoTemplate.count(buildListQuery(recipient, channelType), RecordEntity.class);
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
