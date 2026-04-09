package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;
import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.handler.ChannelProviderHandler;
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
public class ChannelProviderRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/channel-providers", method = RequestMethod.POST, beanClass = ChannelProviderHandler.class, beanMethod = "create", operation = @Operation(operationId = "createChannelProvider", summary = "创建通道商", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChannelProviderDTO.class))), responses = {
                                        @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = ChannelProviderDTO.class))),                        })),
                        @RouterOperation(path = "/v1/channel-providers", method = RequestMethod.GET, beanClass = ChannelProviderHandler.class, beanMethod = "listPage", operation = @Operation(operationId = "listChannelProviders", summary = "通道商分页列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "page", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "1")),
                                        @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = PageResp.class))),                        })),
                        @RouterOperation(path = "/v1/channel-providers/{id}", method = RequestMethod.GET, beanClass = ChannelProviderHandler.class, beanMethod = "getById", operation = @Operation(operationId = "getChannelProviderById", summary = "按 ID 查询通道商", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ChannelProviderDTO.class))),
                                        @ApiResponse(responseCode = "404", description = "未找到"),                        })),
                        @RouterOperation(path = "/v1/channel-providers/{id}", method = RequestMethod.PUT, beanClass = ChannelProviderHandler.class, beanMethod = "update", operation = @Operation(operationId = "updateChannelProvider", summary = "更新通道商", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChannelProviderDTO.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ChannelProviderDTO.class))),
                                        @ApiResponse(responseCode = "404", description = "未找到"),                        })),
                        @RouterOperation(path = "/v1/channel-providers/{id}", method = RequestMethod.DELETE, beanClass = ChannelProviderHandler.class, beanMethod = "delete", operation = @Operation(operationId = "deleteChannelProvider", summary = "删除通道商", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "204", description = "无内容"),                        }))
        })
        public RouterFunction<ServerResponse> channelProviderRoutes(ChannelProviderHandler channelProviderHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/channel-providers")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::create)
                                .andRoute(RequestPredicates.GET("/v1/channel-providers")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::listPage)
                                .andRoute(RequestPredicates.GET("/v1/channel-providers/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::getById)
                                .andRoute(RequestPredicates.PUT("/v1/channel-providers/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::update)
                                .andRoute(RequestPredicates.DELETE("/v1/channel-providers/{id}"),
                                                channelProviderHandler::delete);
        }
}
