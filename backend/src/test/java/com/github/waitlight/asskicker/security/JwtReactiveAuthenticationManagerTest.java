package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtReactiveAuthenticationManagerTest {

    private JwtService jwtService;
    private JwtReactiveAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-for-jwt-signing-32-bytes");
        properties.setAccessTokenTtl(Duration.ofDays(1));
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        jwtService = new JwtService(properties);
        jwtService.init();
        authenticationManager = new JwtReactiveAuthenticationManager(jwtService);
    }

    @Test
    void authenticateAccessTokenWithRole() {
        User user = new User();
        user.setId(7L);
        user.setRole(UserRole.ADMIN);

        String token = jwtService.generateAccessToken(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, token);

        StepVerifier.create(authenticationManager.authenticate(authentication))
                .assertNext(result -> {
                    assertThat(result.isAuthenticated()).isTrue();
                    assertThat(result.getAuthorities())
                            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                })
                .verifyComplete();
    }
}
