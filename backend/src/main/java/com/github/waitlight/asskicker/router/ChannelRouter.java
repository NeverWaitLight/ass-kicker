package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.handlers.ChannelHandler;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.sender.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Channels", description = "渠道管理")
public class ChannelRouter {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/channels",
                    method = RequestMethod.POST,
                    beanClass = ChannelHandler.class,
                    beanMethod = "createChannel",
                    operation = @Operation(
                            operationId = "createChannel",
                            summary = "创建渠道",
                            tags = {"Channels"},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = Channel.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "创建成功",
                                            content = @Content(schema = @Schema(implementation = Channel.class))),
                                    @ApiResponse(responseCode = "400", description = "参数错误"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/channels",
                    method = RequestMethod.GET,
                    beanClass = ChannelHandler.class,
                    beanMethod = "listChannels",
                    operation = @Operation(
                            operationId = "listChannels",
                            summary = "获取渠道列表",
                            tags = {"Channels"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Channel.class)))),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/channels/types",
                    method = RequestMethod.GET,
                    beanClass = ChannelHandler.class,
                    beanMethod = "listChannelTypes",
                    operation = @Operation(
                            operationId = "listChannelTypes",
                            summary = "获取通道类型列表",
                            tags = {"Channels"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/channels/{id}",
                    method = RequestMethod.GET,
                    beanClass = ChannelHandler.class,
                    beanMethod = "getChannelById",
                    operation = @Operation(
                            operationId = "getChannelById",
                            summary = "获取渠道详情",
                            tags = {"Channels"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(schema = @Schema(implementation = Channel.class))),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/channels/{id}",
                    method = RequestMethod.PUT,
                    beanClass = ChannelHandler.class,
                    beanMethod = "updateChannel",
                    operation = @Operation(
                            operationId = "updateChannel",
                            summary = "更新渠道",
                            tags = {"Channels"},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = Channel.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "更新成功",
                                            content = @Content(schema = @Schema(implementation = Channel.class))),
                                    @ApiResponse(responseCode = "400", description = "参数错误"),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/channels/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = ChannelHandler.class,
                    beanMethod = "deleteChannel",
                    operation = @Operation(
                            operationId = "deleteChannel",
                            summary = "删除渠道",
                            tags = {"Channels"},
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "删除成功"),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            )
            ,
            @RouterOperation(
                    path = "/api/channels/test-send",
                    method = RequestMethod.POST,
                    beanClass = ChannelHandler.class,
                    beanMethod = "testSend",
                    operation = @Operation(
                            operationId = "testSendChannel",
                            summary = "测试发送通道",
                            tags = {"Channels"},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = TestSendRequest.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "测试发送成功",
                                            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "参数错误"),
                                    @ApiResponse(responseCode = "401", description = "未授权"),
                                    @ApiResponse(responseCode = "429", description = "请求过于频繁")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> channelRoutes(ChannelHandler channelHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::createChannel)
                .andRoute(RequestPredicates.GET("/api/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::listChannels)
                .andRoute(RequestPredicates.GET("/api/channels/types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::listChannelTypes)
                .andRoute(RequestPredicates.GET("/api/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::getChannelById)
                .andRoute(RequestPredicates.PUT("/api/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::updateChannel)
                .andRoute(RequestPredicates.DELETE("/api/channels/{id}"), channelHandler::deleteChannel)
                .andRoute(RequestPredicates.POST("/api/channels/test-send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::testSend);
    }
}
