package com.github.waitlight.asskicker.config.snowflake;

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
     * 存放 worker 注册记录的集合名。
     */
    private String counterCollection = "snowflake_counters";

    /**
     * 启动时从 Mongo 分配 workerId 的一次性阻塞超时。
     */
    private Duration allocationBlockTimeout = Duration.ofSeconds(30);

    /**
     * 心跳续期间隔（默认 1 分钟）。
     */
    private Duration heartbeatInterval = Duration.ofMinutes(1);

    /**
     * 超过此时间无心跳的注册记录视为已下线，其 workerId 可被新实例复用（默认 3 分钟）。
     */
    private Duration heartbeatTimeout = Duration.ofMinutes(3);
}
