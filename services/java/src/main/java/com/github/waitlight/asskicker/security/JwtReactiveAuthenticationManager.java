package com.github.waitlight.asskicker.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    public JwtReactiveAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> jwtService.parseAccessToken(token))
                .map(payload -> {
                    UserPrincipal principal = new UserPrincipal(payload.userId(), payload.role());
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + payload.role().name()));
                    return (Authentication) new UsernamePasswordAuthenticationToken(principal, token, authorities);
                })
                .onErrorMap(ex -> new BadCredentialsException("Invalid token", ex));
    }
}
