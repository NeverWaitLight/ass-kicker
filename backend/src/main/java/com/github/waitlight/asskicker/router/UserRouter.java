package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.dto.user.CreateUserRequest;
import com.github.waitlight.asskicker.dto.user.ResetPasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdatePasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdateUsernameRequest;
import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.handler.UserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
public class UserRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/users", method = RequestMethod.POST, beanClass = UserHandler.class, beanMethod = "createUser", operation = @Operation(operationId = "createUser", summary = "创建用户", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateUserRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))), })),
                        @RouterOperation(path = "/v1/users", method = RequestMethod.GET, beanClass = UserHandler.class, beanMethod = "listUsers", operation = @Operation(operationId = "listUsers", summary = "用户分页列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "page", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "1")),
                                        @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
                                        @Parameter(name = "keyword", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = PageResp.class))), })),
                        @RouterOperation(path = "/v1/users/{id}", method = RequestMethod.GET, beanClass = UserHandler.class, beanMethod = "getUserById", operation = @Operation(operationId = "getUserById", summary = "按 ID 查询用户", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))), })),
                        @RouterOperation(path = "/v1/users/{id}", method = RequestMethod.DELETE, beanClass = UserHandler.class, beanMethod = "deleteUser", operation = @Operation(operationId = "deleteUser", summary = "删除用户", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "204", description = "无内容"), })),
                        @RouterOperation(path = "/v1/users/{id}/password", method = RequestMethod.PUT, beanClass = UserHandler.class, beanMethod = "resetPassword", operation = @Operation(operationId = "resetPassword", summary = "管理员重置用户密码", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))), })),
                        @RouterOperation(path = "/v1/users/me", method = RequestMethod.PATCH, beanClass = UserHandler.class, beanMethod = "updateMeUsername", operation = @Operation(operationId = "updateMeUsername", summary = "当前用户修改用户名", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UpdateUsernameRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))),
                                        @ApiResponse(responseCode = "401", description = "未认证"), })),
                        @RouterOperation(path = "/v1/users/me/password", method = RequestMethod.PUT, beanClass = UserHandler.class, beanMethod = "updateMePassword", operation = @Operation(operationId = "updateMePassword", summary = "当前用户修改密码", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UpdatePasswordRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = UserView.class))),
                                        @ApiResponse(responseCode = "401", description = "未认证"), }))
        })
        public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/users")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::createUser)
                                .andRoute(RequestPredicates.GET("/v1/users")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::listUsers)
                                .andRoute(RequestPredicates.GET("/v1/users/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::getUserById)
                                .andRoute(RequestPredicates.DELETE("/v1/users/{id}"), userHandler::deleteUser)
                                .andRoute(RequestPredicates.PUT("/v1/users/{id}/password")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::resetPassword)
                                .andRoute(RequestPredicates.PATCH("/v1/users/me")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::updateMeUsername)
                                .andRoute(RequestPredicates.PUT("/v1/users/me/password")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                userHandler::updateMePassword);
        }
}
