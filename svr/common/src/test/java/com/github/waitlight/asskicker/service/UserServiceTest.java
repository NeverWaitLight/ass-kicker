package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
                AssKickerTestApplication.class,
                MongoTestConfiguration.class
}, properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class UserServiceTest {

        @Autowired
        private UserService userService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ReactiveMongoTemplate mongoTemplate;

        @BeforeEach
        void clearUsers() {
                StepVerifier.create(mongoTemplate.dropCollection(UserEntity.class)).verifyComplete();
        }

        @Test
        void create_success() {
                UserEntity input = UserEntityFixtures.memberUser();
                String originalPassword = input.getPassword();

                StepVerifier.create(userService.create(input))
                                .assertNext(saved -> {
                                        assertThat(saved.getId()).isNotBlank();
                                        assertThat(saved.getPassword()).isNotEqualTo(originalPassword);
                                        assertThat(saved.getPassword()).startsWith("$2a$");
                                })
                                .verifyComplete();
        }

        @Test
        void resetPassword_success() {
                UserEntity user = UserEntityFixtures.memberUser();
                String originalPassword = user.getPassword();
                String newPassword = "newPassword456";

                StepVerifier.create(userService.create(user)
                                .flatMap(saved -> userService.resetPassword(saved.getId(), newPassword,
                                                originalPassword)))
                                .assertNext(updated -> assertThat(
                                                passwordEncoder.matches(newPassword, updated.getPassword())).isTrue())
                                .verifyComplete();
        }

        @Test
        void update_success() {
                UserEntity user = UserEntityFixtures.memberUser();

                StepVerifier.create(userService.create(user)
                                .flatMap(saved -> {
                                        UserEntity update = new UserEntity();
                                        update.setId(saved.getId());
                                        update.setUsername("newusername");
                                        update.setPassword(saved.getPassword());
                                        update.setRole(UserRole.ADMIN);
                                        return userService.update(update);
                                }))
                                .assertNext(updated -> assertThat(updated.getUsername()).isEqualTo("newusername"))
                                .verifyComplete();
        }

        @Test
        void delete_success() {
                UserEntity user = UserEntityFixtures.memberUser();

                StepVerifier.create(userService.create(user)
                                .flatMap(saved -> userService.delete(saved.getId())))
                                .verifyComplete();
        }
}
