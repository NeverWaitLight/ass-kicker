package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.apikey.ApiKeyView;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyRequest;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyResponse;
import com.github.waitlight.asskicker.handler.ApiKeyHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
public class ApiKeyRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/api-keys", method = RequestMethod.POST, beanClass = ApiKeyHandler.class, beanMethod = "createApiKey", operation = @Operation(operationId = "createApiKey", summary = "创建 API Key", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = CreateApiKeyRequest.class))), responses = {
                                        @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = CreateApiKeyResponse.class))),                        })),
                        @RouterOperation(path = "/v1/api-keys", method = RequestMethod.GET, beanClass = ApiKeyHandler.class, beanMethod = "listApiKeys", operation = @Operation(operationId = "listApiKeys", summary = "API Key 列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiKeyView.class)))),                        })),
                        @RouterOperation(path = "/v1/api-keys/{id}", method = RequestMethod.DELETE, beanClass = ApiKeyHandler.class, beanMethod = "revokeApiKey", operation = @Operation(operationId = "revokeApiKey", summary = "销毁 API Key", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "204", description = "无内容"),                        }))
        })
        public RouterFunction<ServerResponse> apiKeyRoutes(ApiKeyHandler apiKeyHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/api-keys")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                apiKeyHandler::createApiKey)
                                .andRoute(RequestPredicates.GET("/v1/api-keys")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                apiKeyHandler::listApiKeys)
                                .andRoute(RequestPredicates.DELETE("/v1/api-keys/{id}"), apiKeyHandler::revokeApiKey);
        }
}
