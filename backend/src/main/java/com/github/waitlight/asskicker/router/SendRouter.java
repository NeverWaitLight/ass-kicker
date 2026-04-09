package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.dto.send.SendResponse;
import com.github.waitlight.asskicker.handler.SendHandler;
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
public class SendRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/send", method = RequestMethod.POST, beanClass = SendHandler.class, beanMethod = "send", operation = @Operation(operationId = "send", summary = "同步发送", security = {
                                        @SecurityRequirement(name = OpenApiConfig.BEARER_JWT),
                                        @SecurityRequirement(name = OpenApiConfig.API_KEY)
                        }, requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UniTask.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = SendResponse.class))),                        }))
        })
        public RouterFunction<ServerResponse> sendRoutes(SendHandler sendHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/send")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                sendHandler::send);
        }

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/submit", method = RequestMethod.POST, beanClass = SendHandler.class, beanMethod = "submit", operation = @Operation(operationId = "submit", summary = "异步提交", security = {
                                        @SecurityRequirement(name = OpenApiConfig.BEARER_JWT),
                                        @SecurityRequirement(name = OpenApiConfig.API_KEY)
                        }, requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UniTask.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = SendResponse.class))),                        }))
        })
        public RouterFunction<ServerResponse> submitRoutes(SendHandler sendHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/submit")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                sendHandler::submit);
        }
}
