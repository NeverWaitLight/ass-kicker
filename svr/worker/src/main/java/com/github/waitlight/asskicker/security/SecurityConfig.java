package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.service.ApiKeyService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JwtService jwtService,
                                                         ApiKeyService apiKeyService) {
        JwtReactiveAuthenticationManager jwtManager = new JwtReactiveAuthenticationManager(jwtService);
        ApiKeyReactiveAuthenticationManager apiKeyManager = new ApiKeyReactiveAuthenticationManager(apiKeyService);
        OrReactiveAuthenticationManager combinedManager = new OrReactiveAuthenticationManager(jwtManager, apiKeyManager);

        AuthenticationWebFilter combinedFilter = new AuthenticationWebFilter(combinedManager);
        combinedFilter.setServerAuthenticationConverter(new CombinedServerAuthenticationConverter());
        combinedFilter.setAuthenticationFailureHandler((exchange, ex) -> {
            exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getExchange().getResponse().setComplete();
        });

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/scalar", "/scalar/**", "/webjars/**")
                        .permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/v1/send", "/v1/submit").authenticated()
                        .anyExchange().denyAll())
                .addFilterAt(combinedFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
