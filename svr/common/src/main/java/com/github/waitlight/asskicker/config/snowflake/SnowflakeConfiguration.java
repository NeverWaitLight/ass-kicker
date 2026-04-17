package com.github.waitlight.asskicker.config.snowflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SnowflakeWorkerRegistry;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfiguration implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeConfiguration.class);

    private Disposable heartbeatSubscription;
    private SnowflakeWorkerRegistry workerRegistry;

    @Bean
    public SnowflakeWorkerRegistry snowflakeWorkerRegistry(
            ReactiveMongoTemplate reactiveMongoTemplate,
            SnowflakeProperties props) {
        return new SnowflakeWorkerRegistry(
                reactiveMongoTemplate,
                props.getCounterCollection(),
                props.getHeartbeatTimeout());
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            SnowflakeProperties props,
            SnowflakeWorkerRegistry registry) {

        int datacenterId = props.getDatacenterId();

        final long workerId;
        if (props.getWorkerId() != null) {
            workerId = props.getWorkerId();
            log.info("Snowflake using static workerId={} datacenterId={}", workerId, datacenterId);
        } else if (!props.isMongoAllocationEnabled()) {
            throw new IllegalStateException(
                    "ass-kicker.snowflake.worker-id is required when ass-kicker.snowflake.mongo-allocation-enabled=false");
        } else {
            this.workerRegistry = registry;
            workerId = registry.ensureIndex()
                    .then(registry.register(datacenterId))
                    .block(props.getAllocationBlockTimeout());

            log.info("Snowflake registered workerId={} datacenterId={} instanceId={}",
                    workerId, datacenterId, "uuid");

            Duration interval = props.getHeartbeatInterval();
            this.heartbeatSubscription = Flux.interval(interval)
                    .flatMap(tick -> registry.renewHeartbeat()
                            .doOnError(e -> log.warn("Snowflake heartbeat renewal failed", e))
                            .onErrorComplete())
                    .subscribe(
                            ignored -> {},
                            e -> log.error("Snowflake heartbeat stream terminated with error", e));
        }

        return new SnowflakeIdGenerator(workerId, datacenterId);
    }

    @Override
    public void destroy() {
        if (heartbeatSubscription != null && !heartbeatSubscription.isDisposed()) {
            heartbeatSubscription.dispose();
        }
        if (workerRegistry != null) {
            try {
                workerRegistry.deregister().block(Duration.ofSeconds(5));
                log.info("Snowflake worker registration removed on shutdown");
            } catch (Exception e) {
                log.warn("Failed to deregister Snowflake worker on shutdown", e);
            }
        }
    }
}
