package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.converter.UserConverterImpl;
import com.github.waitlight.asskicker.dto.auth.LoginDTO;
import com.github.waitlight.asskicker.dto.auth.TokenVO;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.exception.UnauthorizedException;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.security.JwtProperties;
import com.github.waitlight.asskicker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
                AssKickerTestApplication.class,
                MongoTestConfiguration.class
}, properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
@Import({ AuthService.class, JwtService.class, UserConverterImpl.class })
@EnableConfigurationProperties(JwtProperties.class)
class AuthServiceTest {

        @Autowired
        private AuthService authService;

        @Autowired
        private UserService userService;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private ReactiveMongoTemplate mongoTemplate;

        @BeforeEach
        void clearUsers() {
                StepVerifier.create(mongoTemplate.dropCollection(UserEntity.class)).verifyComplete();
        }

        @Nested
        class Login {

                @Test
                void login_success() {
                        UserEntity input = UserEntityFixtures.memberUser();
                        String plainPassword = input.getPassword();

                        StepVerifier.create(userService.create(input)
                                        .flatMap(saved -> authService
                                                        .login(new LoginDTO(saved.getUsername(), plainPassword))
                                                        .flatMap(token -> userService.getById(saved.getId())
                                                                        .map(user -> new TokenAndUser(token, user)))))
                                        .assertNext(tu -> {
                                                TokenVO token = tu.token();
                                                UserEntity user = tu.user();
                                                assertThat(token.accessToken()).isNotBlank();
                                                assertThat(token.refreshToken()).isNotBlank();
                                                assertThat(token.accessToken().split("\\.")).hasSize(3);
                                                assertThat(token.refreshToken().split("\\.")).hasSize(3);
                                                assertThat(token.user().username()).isEqualTo("testuser");
                                                assertThat(token.user().role()).isEqualTo(UserRole.MEMBER);
                                                assertThat(user.getLastLoginAt()).isNotNull();
                                        })
                                        .verifyComplete();
                }

                @Test
                void login_fails_when_request_null() {
                        StepVerifier.create(authService.login(null))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(BadRequestException.class)
                                                        .hasMessage("auth.credentials.empty"))
                                        .verify();
                }

                @ParameterizedTest
                @NullAndEmptySource
                @ValueSource(strings = { " ", "\t" })
                void login_fails_when_username_blank(String username) {
                        StepVerifier.create(authService.login(new LoginDTO(username, "password123")))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(BadRequestException.class)
                                                        .hasMessage("auth.credentials.empty"))
                                        .verify();
                }

                @ParameterizedTest
                @NullAndEmptySource
                @ValueSource(strings = { " ", "\t" })
                void login_fails_when_password_blank(String password) {
                        StepVerifier.create(authService.login(new LoginDTO("testuser", password)))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(BadRequestException.class)
                                                        .hasMessage("auth.credentials.empty"))
                                        .verify();
                }

                @Test
                void login_fails_when_user_not_found() {
                        StepVerifier.create(authService.login(new LoginDTO("nouser", "password123")))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(UnauthorizedException.class)
                                                        .satisfies(e -> assertThat(
                                                                        ((UnauthorizedException) e).getMessageKey())
                                                                        .isEqualTo("auth.login.failed")))
                                        .verify();
                }

                @Test
                void login_fails_when_password_wrong() {
                        UserEntity input = UserEntityFixtures.memberUser();

                        StepVerifier.create(userService.create(input)
                                        .flatMap(saved -> authService.login(
                                                        new LoginDTO(saved.getUsername(), "wrongPassword"))))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(UnauthorizedException.class)
                                                        .satisfies(e -> assertThat(
                                                                        ((UnauthorizedException) e).getMessageKey())
                                                                        .isEqualTo("auth.login.failed")))
                                        .verify();
                }

                @Test
                void login_fails_when_user_disabled() {
                        UserEntity input = UserEntityFixtures.disabledUser();

                        StepVerifier.create(userService.create(input)
                                        .flatMap(saved -> {
                                                saved.setStatus(UserStatus.DISABLED);
                                                return mongoTemplate.save(saved);
                                        })
                                        .flatMap(saved -> authService.login(
                                                        new LoginDTO(saved.getUsername(), "disabledpass123"))))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(PermissionDeniedException.class)
                                                        .hasMessage("auth.user.disabled"))
                                        .verify();
                }
        }

        @Nested
        class Refresh {

                @Test
                void refresh_success() {
                        UserEntity input = UserEntityFixtures.memberUser();

                        StepVerifier.create(userService.create(input)
                                        .flatMap(saved -> {
                                                String refresh = jwtService.generateRefreshToken(saved);
                                                return authService.refresh(refresh);
                                        }))
                                        .assertNext(token -> {
                                                assertThat(token.accessToken()).isNotBlank();
                                                assertThat(token.refreshToken()).isNotBlank();
                                                assertThat(token.accessToken().split("\\.")).hasSize(3);
                                                assertThat(token.refreshToken().split("\\.")).hasSize(3);
                                                assertThat(token.user().username()).isEqualTo("testuser");
                                        })
                                        .verifyComplete();
                }

                @ParameterizedTest
                @NullAndEmptySource
                @ValueSource(strings = { " ", "\t" })
                void refresh_fails_when_token_blank(String refreshToken) {
                        StepVerifier.create(authService.refresh(refreshToken))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(BadRequestException.class)
                                                        .hasMessage("auth.refreshToken.empty"))
                                        .verify();
                }

                @Test
                void refresh_fails_when_token_invalid() {
                        StepVerifier.create(authService.refresh("not-a-jwt"))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(UnauthorizedException.class)
                                                        .satisfies(e -> assertThat(
                                                                        ((UnauthorizedException) e).getMessageKey())
                                                                        .isEqualTo("auth.refreshToken.invalid")))
                                        .verify();
                }

                @Test
                void refresh_fails_when_user_not_found() {
                        UserEntity ghost = new UserEntity();
                        ghost.setId("999999999999999999");
                        ghost.setRole(UserRole.MEMBER);
                        String refresh = jwtService.generateRefreshToken(ghost);

                        StepVerifier.create(authService.refresh(refresh))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(UnauthorizedException.class)
                                                        .satisfies(e -> assertThat(
                                                                        ((UnauthorizedException) e).getMessageKey())
                                                                        .isEqualTo("auth.user.notFound")))
                                        .verify();
                }

                @Test
                void refresh_fails_when_user_disabled() {
                        UserEntity input = UserEntityFixtures.disabledUser();

                        StepVerifier.create(userService.create(input)
                                        .flatMap(saved -> {
                                                saved.setStatus(UserStatus.DISABLED);
                                                return mongoTemplate.save(saved);
                                        })
                                        .flatMap(saved -> authService.refresh(
                                                        jwtService.generateRefreshToken(saved))))
                                        .expectErrorSatisfies(ex -> assertThat(ex)
                                                        .isInstanceOf(PermissionDeniedException.class)
                                                        .hasMessage("auth.user.disabled"))
                                        .verify();
                }
        }

        private record TokenAndUser(TokenVO token, UserEntity user) {
        }
}
