package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterPerformanceTest {

    @Test
    @Tag("performance")
    void compareMapStructWithManualMapping() {
        Assumptions.assumeTrue(Boolean.getBoolean("perfTest"));

        int warmup = Integer.getInteger("perfWarmup", 20000);
        int iterations = Integer.getInteger("perfIterations", 200000);

        User user = sampleUser();
        UserConverter mapper = Mappers.getMapper(UserConverter.class);

        runLoop(warmup, () -> manualToView(user));
        runLoop(warmup, () -> mapper.toView(user));

        long manualNs = measure(iterations, () -> manualToView(user));
        long mapstructNs = measure(iterations, () -> mapper.toView(user));

        double manualPerOp = manualNs / (double) iterations;
        double mapstructPerOp = mapstructNs / (double) iterations;

        System.out.printf("Manual mapping: %.2f ns/op, MapStruct: %.2f ns/op%n", manualPerOp, mapstructPerOp);

        assertThat(manualNs).isPositive();
        assertThat(mapstructNs).isPositive();
    }

    private static void runLoop(int iterations, Runnable action) {
        for (int i = 0; i < iterations; i++) {
            action.run();
        }
    }

    private static long measure(int iterations, Runnable action) {
        long start = System.nanoTime();
        runLoop(iterations, action);
        return System.nanoTime() - start;
    }

    private static UserView manualToView(User user) {
        if (user == null) {
            return null;
        }
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }

    private static User sampleUser() {
        User user = new User();
        user.setId(99L);
        user.setUsername("perf");
        user.setPasswordHash("secret");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(1000L);
        user.setUpdatedAt(2000L);
        user.setLastLoginAt(3000L);
        return user;
    }
}
