package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.RefreshRequest;
import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.dto.auth.TokenResponse;
import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.handler.AuthHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class AuthRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/auth/login", method = RequestMethod.POST, beanClass = AuthHandler.class, beanMethod = "login", operation = @Operation(operationId = "login", summary = "登录", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = LoginRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = TokenResponse.class))),                        })),
                        @RouterOperation(path = "/v1/auth/register", method = RequestMethod.POST, beanClass = AuthHandler.class, beanMethod = "register", operation = @Operation(operationId = "register", summary = "注册", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RegisterRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))),                        })),
                        @RouterOperation(path = "/v1/auth/refresh", method = RequestMethod.POST, beanClass = AuthHandler.class, beanMethod = "refresh", operation = @Operation(operationId = "refresh", summary = "刷新令牌", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = RefreshRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = TokenResponse.class))),                        })),
                        @RouterOperation(path = "/v1/auth/me", method = RequestMethod.GET, beanClass = AuthHandler.class, beanMethod = "me", operation = @Operation(operationId = "me", summary = "当前登录用户", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))),
                                        @ApiResponse(responseCode = "401", description = "未认证"),                        }))
        })
        public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/auth/login")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                authHandler::login)
                                .andRoute(RequestPredicates.POST("/v1/auth/register")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                authHandler::register)
                                .andRoute(RequestPredicates.POST("/v1/auth/refresh")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                authHandler::refresh)
                                .andRoute(RequestPredicates.GET("/v1/auth/me")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                authHandler::me);
        }
}
