package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private final UserConverter mapper = Mappers.getMapper(UserConverter.class);

    @Test
    void toViewMapsAllFields() {
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setPasswordHash("secret");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(1000L);
        user.setUpdatedAt(2000L);
        user.setLastLoginAt(3000L);

        UserView view = mapper.toView(user);

        assertThat(view).isNotNull();
        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.username()).isEqualTo("alice");
        assertThat(view.role()).isEqualTo(UserRole.ADMIN);
        assertThat(view.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(view.createdAt()).isEqualTo(1000L);
        assertThat(view.updatedAt()).isEqualTo(2000L);
        assertThat(view.lastLoginAt()).isEqualTo(3000L);
    }

    @Test
    void toEntityIgnoresPasswordHash() {
        UserView view = new UserView(
                12L,
                "bob",
                UserRole.USER,
                UserStatus.ACTIVE,
                111L,
                222L,
                333L
        );

        User user = mapper.toEntity(view);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(12L);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isEqualTo(111L);
        assertThat(user.getUpdatedAt()).isEqualTo(222L);
        assertThat(user.getLastLoginAt()).isEqualTo(333L);
        assertThat(user.getPasswordHash()).isNull();
    }
}
