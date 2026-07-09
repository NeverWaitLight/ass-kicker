package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LocalizedTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class LocalizedTemplateRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<LocalizedTemplateEntity> save(LocalizedTemplateEntity entity) {
        return mongoTemplate.save(entity);
    }

    public Mono<LocalizedTemplateEntity> findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, LocalizedTemplateEntity.class);
    }

    public Mono<LocalizedTemplateEntity> findByTemplateIdAndLanguage(String templateId, Language language) {
        Query query = new Query();
        query.addCriteria(Criteria.where("templateId").is(templateId));
        query.addCriteria(Criteria.where("language").is(language));
        return mongoTemplate.findOne(query, LocalizedTemplateEntity.class);
    }

    public Flux<LocalizedTemplateEntity> findByTemplateId(String templateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("templateId").is(templateId));
        return mongoTemplate.find(query, LocalizedTemplateEntity.class);
    }

    public Mono<Void> deleteById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, LocalizedTemplateEntity.class).then();
    }

    public Mono<Void> deleteByTemplateId(String templateId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("templateId").is(templateId));
        return mongoTemplate.remove(query, LocalizedTemplateEntity.class).then();
    }
}
