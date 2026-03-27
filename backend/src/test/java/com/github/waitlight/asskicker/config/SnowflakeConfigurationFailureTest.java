package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.util.SnowflakeMongoAllocator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

class SnowflakeConfigurationFailureTest {

    @Test
    void snowflakeIdGenerator_mongoDisabledWithoutWorkerId_fails() {
        SnowflakeProperties p = new SnowflakeProperties();
        p.setDatacenterId(1);
        p.setMongoAllocationEnabled(false);

        SnowflakeMongoAllocator allocator = Mockito.mock(SnowflakeMongoAllocator.class);

        assertThatThrownBy(() -> new SnowflakeConfiguration().snowflakeIdGenerator(p, allocator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("worker-id is required");

        verifyNoInteractions(allocator);
    }
}
