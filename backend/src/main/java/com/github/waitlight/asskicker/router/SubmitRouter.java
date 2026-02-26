package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.dto.submit.SubmitRequest;
import com.github.waitlight.asskicker.dto.submit.SubmitResponse;
import com.github.waitlight.asskicker.handlers.SubmitHandler;
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
@Tag(name = "Submit", description = "提交发送任务")
public class SubmitRouter {

    @Bean
    @RouterOperation(
            path = "/api/submit",
            method = RequestMethod.POST,
            beanClass = SubmitHandler.class,
            beanMethod = "submit",
            operation = @Operation(
                    operationId = "submit",
                    summary = "提交发送任务",
                    tags = {"Submit"},
                    requestBody = @RequestBody(required = true,
                            content = @Content(schema = @Schema(implementation = SubmitRequest.class))),
                    responses = {
                            @ApiResponse(responseCode = "200", description = "提交成功，返回任务ID",
                                    content = @Content(schema = @Schema(implementation = SubmitResponse.class))),
                            @ApiResponse(responseCode = "400", description = "参数错误"),
                            @ApiResponse(responseCode = "401", description = "未授权")
                    }
            )
    )
    public RouterFunction<ServerResponse> submitRoutes(SubmitHandler submitHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/submit")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), submitHandler::submit);
    }
}
