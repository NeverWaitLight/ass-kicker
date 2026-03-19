package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.SendRecordEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SendRecordRepository extends ReactiveMongoRepository<SendRecordEntity, String>, SendRecordRepositoryCustom {

    Flux<SendRecordEntity> findByTaskId(String taskId);
}

interface SendRecordRepositoryCustom {

    Flux<SendRecordEntity> findPage(int limit, int offset, String recipient, String channelType);

    Mono<Long> countAll(String recipient, String channelType);
}

class SendRecordRepositoryCustomImpl implements SendRecordRepositoryCustom {

    private final ReactiveMongoTemplate mongoTemplate;

    SendRecordRepositoryCustomImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<SendRecordEntity> findPage(int limit, int offset, String recipient, String channelType) {
        Query query = buildListQuery(recipient, channelType);
        query.with(Sort.by(Sort.Direction.DESC, "sentAt"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, SendRecordEntity.class);
    }

    @Override
    public Mono<Long> countAll(String recipient, String channelType) {
        return mongoTemplate.count(buildListQuery(recipient, channelType), SendRecordEntity.class);
    }

    private Query buildListQuery(String recipient, String channelType) {
        Query query = new Query();
        if (recipient != null && !recipient.isBlank()) {
            String term = recipient.trim();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("recipient").is(term),
                    Criteria.where("recipients").is(term)));
        }
        if (channelType != null && !channelType.isBlank()) {
            query.addCriteria(Criteria.where("channel_type").is(channelType.trim()));
        }
        return query;
    }
}
