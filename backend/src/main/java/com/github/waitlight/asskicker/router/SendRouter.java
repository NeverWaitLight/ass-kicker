package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.dto.submit.SubmitRequest;
import com.github.waitlight.asskicker.dto.submit.SubmitResponse;
import com.github.waitlight.asskicker.handlers.SendHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
@Tag(name = "Send", description = "直接发送（绕过 MQ，同步执行核心发送流程）")
public class SendRouter {

    @Bean
    @RouterOperation(
            path = "/api/send",
            method = RequestMethod.POST,
            beanClass = SendHandler.class,
            beanMethod = "send",
            operation = @Operation(
                    operationId = "send",
                    summary = "直接发送",
                    description = "与 submit 请求体相同，但不经 MQ，直接执行模板渲染与通道发送",
                    tags = {"Send"},
                    requestBody = @RequestBody(required = true,
                            content = @Content(schema = @Schema(implementation = SubmitRequest.class))),
                    responses = {
                            @ApiResponse(responseCode = "200", description = "发送完成，返回任务ID",
                                    content = @Content(schema = @Schema(implementation = SubmitResponse.class))),
                            @ApiResponse(responseCode = "400", description = "参数错误")
                    }
            )
    )
    public RouterFunction<ServerResponse> sendRoutes(SendHandler sendHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), sendHandler::send);
    }
}
