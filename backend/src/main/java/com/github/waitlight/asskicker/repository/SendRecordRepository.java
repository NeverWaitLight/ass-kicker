package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.SendRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SendRecordRepository extends ReactiveMongoRepository<SendRecord, String> {

    Flux<SendRecord> findByTaskId(String taskId);
}
