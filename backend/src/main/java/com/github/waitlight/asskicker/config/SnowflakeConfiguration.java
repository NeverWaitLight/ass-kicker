package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SnowflakeMongoAllocator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfiguration {

    @Bean
    public SnowflakeMongoAllocator snowflakeMongoAllocator(
            ReactiveMongoTemplate reactiveMongoTemplate, SnowflakeProperties snowflakeProperties) {
        return new SnowflakeMongoAllocator(reactiveMongoTemplate, snowflakeProperties);
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            SnowflakeProperties props,
            SnowflakeMongoAllocator snowflakeMongoAllocator) {
        int datacenterId = props.getDatacenterId();

        final long workerId;
        if (props.getWorkerId() != null) {
            workerId = props.getWorkerId();
        } else if (!props.isMongoAllocationEnabled()) {
            throw new IllegalStateException(
                    "ass-kicker.snowflake.worker-id is required when ass-kicker.snowflake.mongo-allocation-enabled=false");
        } else {
            workerId = snowflakeMongoAllocator
                    .allocateWorkerId(datacenterId)
                    .block(props.getAllocationBlockTimeout());
        }

        return new SnowflakeIdGenerator(workerId, datacenterId);
    }
}
