package com.github.waitlight.asskicker.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {

    @Test
    void nextId_isMonotonicAndUniqueWithinProcess() {
        SnowflakeIdGenerator g = new SnowflakeIdGenerator(1, 1);
        long prev = -1L;
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            long id = g.nextId();
            assertThat(id).isGreaterThan(prev);
            assertThat(seen.add(id)).isTrue();
            prev = id;
        }
    }

    @Test
    void nextIdString_isDecimalFormOfGeneratedId() {
        SnowflakeIdGenerator g = new SnowflakeIdGenerator(2, 3);
        long first = g.nextId();
        String s = g.nextIdString();
        long parsed = Long.parseLong(s);
        long after = g.nextId();
        assertThat(parsed).isGreaterThan(first);
        assertThat(after).isGreaterThan(parsed);
    }
}
