package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.repository.RegistrationLock;
import com.github.waitlight.asskicker.repository.UserQueryRepository;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserQueryRepository userQueryRepository;

    @Mock
    private RegistrationLock registrationLock;

    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private UserConverter userMapStructer;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userMapStructer = Mappers.getMapper(UserConverter.class);
        userService = new UserServiceImpl(userRepository, userQueryRepository, passwordEncoder, registrationLock, userMapStructer);
    }

    @Test
    void registerFirstUserAssignsAdmin() {
        when(registrationLock.acquire()).thenReturn(Mono.empty());
        when(userRepository.existsByUsername("first")).thenReturn(Mono.just(false));
        when(userRepository.count()).thenReturn(Mono.just(0L));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.registerUser(new RegisterRequest("first", "pass")))
                .assertNext(view -> assertThat(view.role()).isEqualTo(UserRole.ADMIN))
                .verifyComplete();
    }

    @Test
    void registerSecondUserDefaultsToUserRole() {
        when(registrationLock.acquire()).thenReturn(Mono.empty());
        when(userRepository.existsByUsername("second")).thenReturn(Mono.just(false));
        when(userRepository.count()).thenReturn(Mono.just(2L));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.registerUser(new RegisterRequest("second", "pass")))
                .assertNext(view -> assertThat(view.role()).isEqualTo(UserRole.USER))
                .verifyComplete();
    }

    @Test
    void registerDuplicateUsernameReturnsConflict() {
        when(registrationLock.acquire()).thenReturn(Mono.empty());
        when(userRepository.existsByUsername("dup")).thenReturn(Mono.just(true));

        StepVerifier.create(userService.registerUser(new RegisterRequest("dup", "pass")))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException response = (ResponseStatusException) ex;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
    }
}
