package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordView;
import com.github.waitlight.asskicker.handler.SendRecordHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class SendRecordRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/send-records", method = RequestMethod.GET, beanClass = SendRecordHandler.class, beanMethod = "listRecords", operation = @Operation(operationId = "listSendRecords", summary = "发送记录分页列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "page", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "1")),
                                        @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
                                        @Parameter(name = "recipient", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
                                        @Parameter(name = "channelType", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = PageResp.class))),                        })),
                        @RouterOperation(path = "/v1/send-records/{id}", method = RequestMethod.GET, beanClass = SendRecordHandler.class, beanMethod = "getRecordById", operation = @Operation(operationId = "getSendRecordById", summary = "按 ID 查询发送记录", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = SendRecordView.class))),                        }))
        })
        public RouterFunction<ServerResponse> sendRecordRoutes(SendRecordHandler sendRecordHandler) {
                return RouterFunctions
                                .route(RequestPredicates.GET("/v1/send-records")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                sendRecordHandler::listRecords)
                                .andRoute(RequestPredicates.GET("/v1/send-records/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                sendRecordHandler::getRecordById);
        }
}
