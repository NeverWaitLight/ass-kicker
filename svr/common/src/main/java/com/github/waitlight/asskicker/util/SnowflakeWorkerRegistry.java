package com.github.waitlight.asskicker.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Mono;

/**
 * 基于 MongoDB 的 Snowflake worker id 注册表。
 * <p>
 * 每个实例启动时生成 UUID 作为唯一标识，通过线性探测找到空闲 workerId 并写入集合；
 * 此后定期刷新 lastHeartbeat；优雅关闭时删除自身记录。
 * 超过 heartbeatTimeout 未续期的记录视为已下线，可被新实例复用。
 */
public final class SnowflakeWorkerRegistry {

    private static final int MAX_WORKER_ID = 31;

    private final ReactiveMongoTemplate mongoTemplate;
    private final String collection;
    private final Duration heartbeatTimeout;

    /** 本实例的唯一标识，重启后更换 */
    private final String instanceId = UUID.randomUUID().toString();

    /** 注册成功后持有，用于心跳续期和反注册 */
    private volatile long registeredWorkerId = -1;

    public SnowflakeWorkerRegistry(ReactiveMongoTemplate mongoTemplate,
                                   String collection,
                                   Duration heartbeatTimeout) {
        this.mongoTemplate = mongoTemplate;
        this.collection = collection;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    /**
     * 确保集合上存在 {datacenterId, workerId} 唯一索引，应在注册前调用一次。
     */
    public Mono<Void> ensureIndex() {
        return mongoTemplate.indexOps(collection)
                .ensureIndex(new Index()
                        .on("datacenterId", Direction.ASC)
                        .on("workerId", Direction.ASC)
                        .unique())
                .then();
    }

    /**
     * 线性探测注册：查询存活记录 → 找空闲 id → 原子插入。
     * 并发抢占时（DuplicateKeyException）跳过该 id 继续探测。
     *
     * @param datacenterId 机房 ID
     * @return 分配到的 workerId
     */
    public Mono<Long> register(int datacenterId) {
        return queryOccupied(datacenterId)
                .flatMap(occupied -> probe(datacenterId, occupied, 0));
    }

    private Mono<Set<Integer>> queryOccupied(int datacenterId) {
        long threshold = Instant.now().minus(heartbeatTimeout).toEpochMilli();
        Query query = Query.query(
                Criteria.where("datacenterId").is(datacenterId)
                        .and("lastHeartbeat").gte(threshold));
        return mongoTemplate.find(query, Document.class, collection)
                .map(doc -> doc.getInteger("workerId"))
                .collect(Collectors.toSet());
    }

    private Mono<Long> probe(int datacenterId, Set<Integer> occupied, int candidate) {
        if (candidate > MAX_WORKER_ID) {
            return Mono.error(new IllegalStateException(
                    "Snowflake worker id pool exhausted for datacenter " + datacenterId
                            + " (max " + (MAX_WORKER_ID + 1) + " workers per datacenter)"));
        }
        if (occupied.contains(candidate)) {
            return probe(datacenterId, occupied, candidate + 1);
        }
        return tryInsert(datacenterId, candidate)
                .onErrorResume(DuplicateKeyException.class,
                        ex -> probe(datacenterId, occupied, candidate + 1));
    }

    private Mono<Long> tryInsert(int datacenterId, int workerId) {
        Document doc = new Document();
        doc.put("_id", instanceId);
        doc.put("datacenterId", datacenterId);
        doc.put("workerId", workerId);
        doc.put("lastHeartbeat", Instant.now().toEpochMilli());

        return mongoTemplate.insert(doc, collection)
                .doOnSuccess(saved -> this.registeredWorkerId = workerId)
                .thenReturn((long) workerId);
    }

    /**
     * 更新本实例的 lastHeartbeat 时间戳。
     */
    public Mono<Void> renewHeartbeat() {
        Query query = Query.query(Criteria.where("_id").is(instanceId));
        Update update = new Update().set("lastHeartbeat", Instant.now().toEpochMilli());
        return mongoTemplate.updateFirst(query, update, collection).then();
    }

    /**
     * 删除本实例的注册记录（优雅关闭时调用）。
     */
    public Mono<Void> deregister() {
        Query query = Query.query(Criteria.where("_id").is(instanceId));
        return mongoTemplate.remove(query, collection).then();
    }

    public long getRegisteredWorkerId() {
        return registeredWorkerId;
    }
}
