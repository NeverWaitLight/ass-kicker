package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.snowflake.SnowflakeConfiguration;
import com.github.waitlight.asskicker.config.snowflake.SnowflakeProperties;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AssKickerTestApplication.class, SnowflakeConfiguration.class})
@TestPropertySource(
        properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14",
                "ass-kicker.snowflake.datacenter-id=9",
                "ass-kicker.snowflake.mongo-allocation-enabled=true",
                "ass-kicker.snowflake.counter-collection=snowflake_counters"
        })
class SnowflakeIdGeneratorMongoAllocationTest {

    @Autowired
    private SnowflakeProperties snowflakeProperties;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void clearCounters() {
        StepVerifier.create(mongoTemplate.dropCollection("snowflake_counters")).verifyComplete();
    }

    @Test
    void startup_allocatesWorkerZeroFromMongo() {
        assertThat(snowflakeProperties.getWorkerId()).isNull();
        assertThat(snowflakeIdGenerator.nextId()).isPositive();
    }
}
