package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handler.StatusHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration(proxyBeanMethods = false)
public class StatusRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/status", method = RequestMethod.GET, beanClass = StatusHandler.class, beanMethod = "status", operation = @Operation(operationId = "status", summary = "服务状态", responses = {
                                        @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(type = "object", example = "{\"service\":\"ass-kicker\",\"status\":\"OK\"}")))
                        }))
        })
        public RouterFunction<ServerResponse> statusRoutes(StatusHandler statusHandler) {
                return RouterFunctions.route(GET("/status"), statusHandler::status);
        }
}
