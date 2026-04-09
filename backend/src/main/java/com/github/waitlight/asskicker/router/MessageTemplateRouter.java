package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.handler.MessageTemplateHandler;
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
public class MessageTemplateRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/v1/message-templates", method = RequestMethod.POST, beanClass = MessageTemplateHandler.class, beanMethod = "create", operation = @Operation(operationId = "createMessageTemplate", summary = "创建消息模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))), responses = {
                                        @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))),                        })),
                        @RouterOperation(path = "/v1/message-templates", method = RequestMethod.GET, beanClass = MessageTemplateHandler.class, beanMethod = "listPage", operation = @Operation(operationId = "listMessageTemplates", summary = "消息模板分页列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "page", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "1")),
                                        @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = PageResp.class))),                        })),
                        @RouterOperation(path = "/v1/message-templates/code/{code}", method = RequestMethod.GET, beanClass = MessageTemplateHandler.class, beanMethod = "getByCode", operation = @Operation(operationId = "getMessageTemplateByCode", summary = "按模板编码查询", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "code", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))),
                                        @ApiResponse(responseCode = "404", description = "未找到"),                        })),
                        @RouterOperation(path = "/v1/message-templates/{id}", method = RequestMethod.GET, beanClass = MessageTemplateHandler.class, beanMethod = "getById", operation = @Operation(operationId = "getMessageTemplateById", summary = "按 ID 查询消息模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))),
                                        @ApiResponse(responseCode = "404", description = "未找到"),                        })),
                        @RouterOperation(path = "/v1/message-templates/{id}", method = RequestMethod.PUT, beanClass = MessageTemplateHandler.class, beanMethod = "update", operation = @Operation(operationId = "updateMessageTemplate", summary = "更新消息模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = MessageTemplateDTO.class))),
                                        @ApiResponse(responseCode = "404", description = "未找到"),                        })),
                        @RouterOperation(path = "/v1/message-templates/{id}", method = RequestMethod.DELETE, beanClass = MessageTemplateHandler.class, beanMethod = "delete", operation = @Operation(operationId = "deleteMessageTemplate", summary = "删除消息模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT), parameters = {
                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, schema = @Schema(type = "string"))
                        }, responses = {
                                        @ApiResponse(responseCode = "204", description = "无内容"),                        }))
        })
        public RouterFunction<ServerResponse> messageTemplateRoutes(MessageTemplateHandler messageTemplateHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/message-templates")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                messageTemplateHandler::create)
                                .andRoute(RequestPredicates.GET("/v1/message-templates")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                messageTemplateHandler::listPage)
                                .andRoute(RequestPredicates.GET("/v1/message-templates/code/{code}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                messageTemplateHandler::getByCode)
                                .andRoute(RequestPredicates.GET("/v1/message-templates/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                messageTemplateHandler::getById)
                                .andRoute(RequestPredicates.PUT("/v1/message-templates/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                messageTemplateHandler::update)
                                .andRoute(RequestPredicates.DELETE("/v1/message-templates/{id}"),
                                                messageTemplateHandler::delete);
        }
}
