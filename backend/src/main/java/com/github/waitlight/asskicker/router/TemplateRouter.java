package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.TemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class TemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> templateRoutes(TemplateHandler templateHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::listTemplates)
                .andRoute(RequestPredicates.POST("/api/templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::createTemplate)
                .andRoute(RequestPredicates.POST("/api/templates/fill")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::fillTemplate)
                .andRoute(RequestPredicates.GET("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::getTemplateById)
                .andRoute(RequestPredicates.GET("/api/templates/code/{code}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::getTemplateByCode)
                .andRoute(RequestPredicates.PUT("/api/templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::updateTemplate)
                .andRoute(RequestPredicates.DELETE("/api/templates/{id}"), templateHandler::deleteTemplate)
                // Routes for language-specific template content
                .andRoute(RequestPredicates.GET("/api/templates/{templateId}/languages/{language}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::getTemplateContentByLanguage)
                .andRoute(RequestPredicates.POST("/api/templates/{templateId}/languages/{language}")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), templateHandler::saveTemplateContentByLanguage)
                .andRoute(RequestPredicates.GET("/api/templates/{templateId}/contents")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), templateHandler::getAllTemplateContentsByTemplateId);
    }
}