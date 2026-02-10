package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.security.JwtProperties;
import com.github.waitlight.asskicker.security.JwtService;
import com.github.waitlight.asskicker.service.impl.AuthServiceImpl;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;
    private UserConverter userMapStructer;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-for-jwt-signing-32-bytes");
        properties.setAccessTokenTtl(Duration.ofDays(1));
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        jwtService = new JwtService(properties);
        jwtService.init();
        userMapStructer = Mappers.getMapper(UserConverter.class);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, userMapStructer);
    }

    @Test
    void loginSuccess() {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setPasswordHash(passwordEncoder.encode("pass"));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByUsername("demo")).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(authService.login(new LoginRequest("demo", "pass")))
                .assertNext(response -> {
                    assertThat(response.accessToken()).isNotBlank();
                    assertThat(response.refreshToken()).isNotBlank();
                    assertThat(response.user().username()).isEqualTo("demo");
                })
                .verifyComplete();
    }

    @Test
    void loginDisabledUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("disabled");
        user.setPasswordHash(passwordEncoder.encode("pass"));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.DISABLED);

        when(userRepository.findByUsername("disabled")).thenReturn(Mono.just(user));

        StepVerifier.create(authService.login(new LoginRequest("disabled", "pass")))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException response = (ResponseStatusException) ex;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .verify();
    }

    @Test
    void refreshTokenSuccess() {
        User user = new User();
        user.setId(3L);
        user.setUsername("refresh");
        user.setPasswordHash(passwordEncoder.encode("pass"));
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        String refreshToken = jwtService.generateRefreshToken(user);
        when(userRepository.findById(3L)).thenReturn(Mono.just(user));

        StepVerifier.create(authService.refresh(refreshToken))
                .assertNext(response -> {
                    assertThat(response.accessToken()).isNotBlank();
                    assertThat(response.refreshToken()).isNotBlank();
                    assertThat(response.user().role()).isEqualTo(UserRole.ADMIN);
                })
                .verifyComplete();
    }
}
