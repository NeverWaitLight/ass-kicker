package com.github.waitlight.asskicker.util;

import com.github.waitlight.asskicker.config.SnowflakeProperties;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Mono;

/**
 * 使用 MongoDB 原子 findAndModify 为指定机房递增分配 workerId（0–31）。
 * <p>
 * 计数文档 {@code _id = snowflake:worker:dc:{datacenterId}}，字段 {@code seq} 表示已占用的
 * 递增次数：分配到的 workerId = seq - 1（首次 upsert 后 seq 为 1 对应 worker 0）。
 */
public final class SnowflakeMongoAllocator {

    static final String WORKER_DOC_ID_PREFIX = "snowflake:worker:dc:";

    /**
     * 与 {@link SnowflakeIdGenerator} 中 5 位 worker 一致：合法 id 为 0..31，seq 合法为 1..32。
     */
    private static final long MAX_SEQ = 32L;

    private final ReactiveMongoTemplate mongoTemplate;
    private final SnowflakeProperties properties;

    public SnowflakeMongoAllocator(ReactiveMongoTemplate mongoTemplate, SnowflakeProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    /**
     * 为给定 {@code datacenterId} 从 Mongo 分配下一个 workerId。
     */
    public Mono<Long> allocateWorkerId(long datacenterId) {
        String docId = WORKER_DOC_ID_PREFIX + datacenterId;
        String collection = properties.getCounterCollection();
        Query query = Query.query(Criteria.where("_id").is(docId));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

        return mongoTemplate
                .findAndModify(query, update, options, Document.class, collection)
                .flatMap(doc -> {
                    long seq = doc.get("seq", Number.class).longValue();
                    if (seq > MAX_SEQ) {
                        return rollbackOverflow(docId, collection).then(Mono.error(new IllegalStateException(
                                "Snowflake worker id pool exhausted for datacenter "
                                        + datacenterId
                                        + " (max 32 workers per datacenter); clear or adjust "
                                        + collection
                                        + " counter document _id="
                                        + docId)));
                    }
                    return Mono.just(seq - 1);
                });
    }

    private Mono<Document> rollbackOverflow(String docId, String collection) {
        Query q = Query.query(Criteria.where("_id").is(docId));
        Update u = new Update().inc("seq", -1);
        FindAndModifyOptions opts = new FindAndModifyOptions().returnNew(true);
        return mongoTemplate.findAndModify(q, u, opts, Document.class, collection);
    }
}
