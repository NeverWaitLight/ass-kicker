package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.dto.sender.TestSendRequest;
import com.github.waitlight.asskicker.handlers.SenderHandler;
import com.github.waitlight.asskicker.model.Sender;
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
@Tag(name = "Senders", description = "发送端管理")
public class SenderRouter {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/senders",
                    method = RequestMethod.POST,
                    beanClass = SenderHandler.class,
                    beanMethod = "createSender",
                    operation = @Operation(
                            operationId = "createSender",
                            summary = "创建发送端",
                            tags = {"Senders"},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = Sender.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "创建成功",
                                            content = @Content(schema = @Schema(implementation = Sender.class))),
                                    @ApiResponse(responseCode = "400", description = "参数错误"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders",
                    method = RequestMethod.GET,
                    beanClass = SenderHandler.class,
                    beanMethod = "listSenders",
                    operation = @Operation(
                            operationId = "listSenders",
                            summary = "获取发送端列表",
                            tags = {"Senders"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Sender.class)))),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders/types",
                    method = RequestMethod.GET,
                    beanClass = SenderHandler.class,
                    beanMethod = "listSenderTypes",
                    operation = @Operation(
                            operationId = "listSenderTypes",
                            summary = "获取发送端类型列表",
                            tags = {"Senders"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders/{id}",
                    method = RequestMethod.GET,
                    beanClass = SenderHandler.class,
                    beanMethod = "getSenderById",
                    operation = @Operation(
                            operationId = "getSenderById",
                            summary = "获取发送端详情",
                            tags = {"Senders"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "成功",
                                            content = @Content(schema = @Schema(implementation = Sender.class))),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders/{id}",
                    method = RequestMethod.PUT,
                    beanClass = SenderHandler.class,
                    beanMethod = "updateSender",
                    operation = @Operation(
                            operationId = "updateSender",
                            summary = "更新发送端",
                            tags = {"Senders"},
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = Sender.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "更新成功",
                                            content = @Content(schema = @Schema(implementation = Sender.class))),
                                    @ApiResponse(responseCode = "400", description = "参数错误"),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = SenderHandler.class,
                    beanMethod = "deleteSender",
                    operation = @Operation(
                            operationId = "deleteSender",
                            summary = "删除发送端",
                            tags = {"Senders"},
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "删除成功"),
                                    @ApiResponse(responseCode = "404", description = "未找到"),
                                    @ApiResponse(responseCode = "401", description = "未授权")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/senders/test-send",
                    method = RequestMethod.POST,
                    beanClass = SenderHandler.class,
                    beanMethod = "testSend",
                    operation = @Operation(
                            operationId = "testSendSender",
                            summary = "测试发送",
                            tags = {"Senders"},
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
    public RouterFunction<ServerResponse> senderRoutes(SenderHandler senderHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/senders")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::createSender)
                .andRoute(RequestPredicates.GET("/api/senders")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::listSenders)
                .andRoute(RequestPredicates.GET("/api/senders/types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::listSenderTypes)
                .andRoute(RequestPredicates.GET("/api/senders/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::getSenderById)
                .andRoute(RequestPredicates.PUT("/api/senders/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::updateSender)
                .andRoute(RequestPredicates.DELETE("/api/senders/{id}"), senderHandler::deleteSender)
                .andRoute(RequestPredicates.POST("/api/senders/test-send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), senderHandler::testSend);
    }
}
