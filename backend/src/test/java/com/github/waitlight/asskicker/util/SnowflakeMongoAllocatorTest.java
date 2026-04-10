package com.github.waitlight.asskicker.util;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.snowflake.SnowflakeConfiguration;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {AssKickerTestApplication.class, SnowflakeConfiguration.class})
@TestPropertySource(
        properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14",
                "ass-kicker.snowflake.datacenter-id=5",
                "ass-kicker.snowflake.worker-id=0",
                "ass-kicker.snowflake.mongo-allocation-enabled=true",
                "ass-kicker.snowflake.counter-collection=snowflake_counters"
        })
class SnowflakeMongoAllocatorTest {

    private static final int DC = 5;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Autowired
    private SnowflakeMongoAllocator allocator;

    @BeforeEach
    void clearCounters() {
        StepVerifier.create(mongoTemplate.dropCollection("snowflake_counters")).verifyComplete();
    }

    @Test
    void allocateWorkerId_sequentialFromZero() {
        StepVerifier.create(allocator.allocateWorkerId(DC)).expectNext(0L).verifyComplete();
        StepVerifier.create(allocator.allocateWorkerId(DC)).expectNext(1L).verifyComplete();
        StepVerifier.create(allocator.allocateWorkerId(DC)).expectNext(2L).verifyComplete();
    }

    @Test
    void allocateWorkerId_differentDatacentersIndependent() {
        StepVerifier.create(allocator.allocateWorkerId(1)).expectNext(0L).verifyComplete();
        StepVerifier.create(allocator.allocateWorkerId(2)).expectNext(0L).verifyComplete();
    }

    @Test
    void allocateWorkerId_thirtyThirdFailsAndRollsBack() {
        String docId = SnowflakeMongoAllocator.WORKER_DOC_ID_PREFIX + DC;
        Document seed = new Document("_id", docId).append("seq", 32L);
        StepVerifier.create(mongoTemplate.insert(seed, "snowflake_counters")).expectNextCount(1).verifyComplete();

        assertThatThrownBy(() -> allocator.allocateWorkerId(DC).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("worker id pool exhausted");

        Document after =
                mongoTemplate.findById(docId, Document.class, "snowflake_counters").block();
        assertThat(after.get("seq", Number.class).longValue()).isEqualTo(32L);
    }
}
