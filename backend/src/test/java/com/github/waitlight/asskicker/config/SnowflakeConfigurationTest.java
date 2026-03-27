package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AssKickerTestApplication.class, SnowflakeConfiguration.class})
@TestPropertySource(
        properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14",
                "ass-kicker.snowflake.datacenter-id=2",
                "ass-kicker.snowflake.worker-id=7",
                "ass-kicker.snowflake.mongo-allocation-enabled=true"
        })
class SnowflakeConfigurationTest {

    @Autowired
    private SnowflakeProperties snowflakeProperties;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Test
    void configuredWorkerId_skipsMongoAllocation() {
        assertThat(snowflakeProperties.getWorkerId()).isEqualTo(7);
        assertThat(snowflakeProperties.getDatacenterId()).isEqualTo(2);
        assertThat(snowflakeIdGenerator.nextId()).isPositive();
    }
}
