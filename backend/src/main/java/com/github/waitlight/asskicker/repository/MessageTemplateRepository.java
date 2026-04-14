package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;

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
public class MessageTemplateRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<MessageTemplateEntity> save(MessageTemplateEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<MessageTemplateEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, MessageTemplateEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, MessageTemplateEntity.class).then();
    }

    public Mono<MessageTemplateEntity> findByCode(String code) {
        Query query = new Query();
        query.addCriteria(Criteria.where("code").is(code));
        return mongoTemplate.findOne(query, MessageTemplateEntity.class);
    }

    public Mono<MessageTemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("code").is(code));
        query.addCriteria(Criteria.where("channelType").is(channelType));
        return mongoTemplate.findOne(query, MessageTemplateEntity.class);
    }

    public Flux<MessageTemplateEntity> findByChannelType(ChannelType channelType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("channelType").is(channelType));
        return mongoTemplate.find(query, MessageTemplateEntity.class);
    }

    public Flux<MessageTemplateEntity> list(String keyword, int limit, int offset) {
        Query query = buildKeywordQuery(keyword);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        query.skip(offset).limit(limit);
        return mongoTemplate.find(query, MessageTemplateEntity.class);
    }

    public Mono<Void> deleteAll() {
        return mongoTemplate.remove(new Query(), MessageTemplateEntity.class).then();
    }

    public Mono<Long> count(String keyword) {
        Query query = buildKeywordQuery(keyword);
        return mongoTemplate.count(query, MessageTemplateEntity.class);
    }

    private Query buildKeywordQuery(String keyword) {
        Query query = new Query();
        if (StringUtils.hasText(keyword)) {
            query.addCriteria(Criteria.where("code").regex(".*" + keyword + ".*", "i"));
        }
        return query;
    }
}
