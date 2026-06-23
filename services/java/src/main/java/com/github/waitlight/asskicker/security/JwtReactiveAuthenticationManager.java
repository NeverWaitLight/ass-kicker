package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.service.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtReactiveAuthenticationManager(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> jwtService.parseAccessToken(token))
                .flatMap(payload -> userService.getById(payload.userId())
                        .flatMap(user -> {
                            if (user.getKickedOutAt() != null && payload.issuedAt() != null
                                    && payload.issuedAt() < user.getKickedOutAt()) {
                                return Mono.error(new BadCredentialsException("Token has been revoked"));
                            }
                            UserPrincipal principal = new UserPrincipal(payload.userId(), payload.role());
                            List<SimpleGrantedAuthority> authorities =
                                    List.of(new SimpleGrantedAuthority("ROLE_" + payload.role().name()));
                            return Mono.just((Authentication) new UsernamePasswordAuthenticationToken(principal, token, authorities));
                        }))
                .onErrorMap(ex -> ex instanceof BadCredentialsException ? ex : new BadCredentialsException("Invalid token", ex));
    }
}
