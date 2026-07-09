package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ProviderTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ProviderTemplateRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<ProviderTemplateEntity> save(ProviderTemplateEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<ProviderTemplateEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, ProviderTemplateEntity.class);
    }

    public Mono<ProviderTemplateEntity> findByLocalizedTemplateIdAndProvider(String localizedTemplateId, ChannelProvider provider) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localizedTemplateId").is(localizedTemplateId));
        query.addCriteria(Criteria.where("provider").is(provider));
        return mongoTemplate.findOne(query, ProviderTemplateEntity.class);
    }

    public Flux<ProviderTemplateEntity> findByLocalizedTemplateId(String localizedTemplateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localizedTemplateId").is(localizedTemplateId));
        return mongoTemplate.find(query, ProviderTemplateEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, ProviderTemplateEntity.class).then();
    }

    public Mono<Void> deleteByLocalizedTemplateId(String localizedTemplateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("localizedTemplateId").is(localizedTemplateId));
        return mongoTemplate.remove(query, ProviderTemplateEntity.class).then();
    }
}
