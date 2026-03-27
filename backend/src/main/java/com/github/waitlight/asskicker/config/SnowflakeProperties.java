package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "ass-kicker.snowflake")
public class SnowflakeProperties {

    /**
     * 机房 ID（0–31），与部署环境绑定写死。
     */
    @NotNull
    @Min(0)
    @Max(31)
    private Integer datacenterId;

    /**
     * 机器 ID（0–31）。未设置时由 Mongo 在启动时递增分配（需开启 {@link #mongoAllocationEnabled}）。
     */
    @Min(0)
    @Max(31)
    private Integer workerId;

    /**
     * 为 false 时必须配置 {@link #workerId}，否则启动失败。
     */
    private boolean mongoAllocationEnabled = true;

    /**
     * 存放 worker 计数器的集合名。
     */
    private String counterCollection = "snowflake_counters";

    /**
     * 启动时从 Mongo 分配 workerId 的一次性阻塞超时。
     */
    private Duration allocationBlockTimeout = Duration.ofSeconds(30);
}
