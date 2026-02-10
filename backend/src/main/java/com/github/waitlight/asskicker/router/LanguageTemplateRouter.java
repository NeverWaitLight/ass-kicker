package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.LanguageTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class LanguageTemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> languageTemplateRoutes(LanguageTemplateHandler languageTemplateHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/language-templates/template/{templateId}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), languageTemplateHandler::getLanguageTemplatesByTemplateId)
                .andRoute(RequestPredicates.GET("/api/language-templates/template/{templateId}/language/{language}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), languageTemplateHandler::getLanguageTemplateByTemplateIdAndLanguage)
                .andRoute(RequestPredicates.POST("/api/language-templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), languageTemplateHandler::createLanguageTemplate)
                .andRoute(RequestPredicates.PUT("/api/language-templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), languageTemplateHandler::updateLanguageTemplate)
                .andRoute(RequestPredicates.DELETE("/api/language-templates/{id}"), languageTemplateHandler::deleteLanguageTemplate);
    }
}