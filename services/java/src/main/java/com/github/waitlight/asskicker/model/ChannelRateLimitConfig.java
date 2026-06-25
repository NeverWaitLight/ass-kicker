package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelRateLimitConfig {

    @Builder.Default
    private boolean enabled = false;

    @Min(1)
    private Integer permitsPerSecond;

    @Min(1)
    private Integer burstCapacity;

    @AssertTrue(message = "{channel.rateLimit.permitsPerSecond.required}")
    @JsonIgnore
    public boolean isPermitsPerSecondPresentWhenEnabled() {
        return !enabled || permitsPerSecond != null;
    }

    @AssertTrue(message = "{channel.rateLimit.burstCapacity.gtePermitsPerSecond}")
    @JsonIgnore
    public boolean isBurstCapacityGreaterThanOrEqualToPermitsPerSecond() {
        return !enabled
                || burstCapacity == null
                || permitsPerSecond == null
                || burstCapacity >= permitsPerSecond;
    }
}
