package com.github.waitlight.asskicker.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.service.TemplateService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.Writer;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateManagerTest {

    @Test
    void fill_sameContentShouldReuseCompiledMustache() {
        TemplateService templateService = mock(TemplateService.class);
        MustacheFactory mustacheFactory = mock(MustacheFactory.class);
        Mustache mustache = mock(Mustache.class);
        TemplateManager templateManager = new TemplateManager(templateService, mustacheFactory,
                Caffeine.newBuilder().build());

        TemplateEntity template = new TemplateEntity("Test", "tpl-code", "desc");
        template.setId("tpl-1");
        LanguageTemplateEntity languageTemplate = new LanguageTemplateEntity("tpl-1", Language.EN, "Hello {{name}}");

        when(templateService.findByCode("tpl-code")).thenReturn(Mono.just(template));
        when(templateService.getTemplateContentByLanguage("tpl-1", Language.EN)).thenReturn(Mono.just(languageTemplate));
        when(mustacheFactory.compile(any(), anyString())).thenReturn(mustache);
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            writer.write("Hello " + params.get("name"));
            return writer;
        }).when(mustache).execute(any(Writer.class), any(Object.class));

        StepVerifier.create(templateManager.fill("tpl-code", Language.EN, Map.of("name", "Alice")))
                .expectNext("Hello Alice")
                .verifyComplete();

        StepVerifier.create(templateManager.fill("tpl-code", Language.EN, Map.of("name", "Bob")))
                .expectNext("Hello Bob")
                .verifyComplete();

        verify(mustacheFactory, times(1)).compile(any(), anyString());
        verify(mustache, times(2)).execute(any(Writer.class), any(Object.class));
    }

    @Test
    void fill_contentChangedShouldCompileAgain() {
        TemplateService templateService = mock(TemplateService.class);
        MustacheFactory mustacheFactory = mock(MustacheFactory.class);
        Mustache firstMustache = mock(Mustache.class);
        Mustache secondMustache = mock(Mustache.class);
        TemplateManager templateManager = new TemplateManager(templateService, mustacheFactory,
                Caffeine.newBuilder().build());

        TemplateEntity template = new TemplateEntity("Test", "tpl-code", "desc");
        template.setId("tpl-1");
        LanguageTemplateEntity firstLanguageTemplate = new LanguageTemplateEntity("tpl-1", Language.EN, "Hello {{name}}");
        LanguageTemplateEntity secondLanguageTemplate = new LanguageTemplateEntity("tpl-1", Language.EN, "Hi {{name}}");

        when(templateService.findByCode("tpl-code")).thenReturn(Mono.just(template));
        when(templateService.getTemplateContentByLanguage("tpl-1", Language.EN))
                .thenReturn(Mono.just(firstLanguageTemplate), Mono.just(secondLanguageTemplate));
        when(mustacheFactory.compile(any(), anyString())).thenReturn(firstMustache, secondMustache);
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(0);
            writer.write("Hello Alice");
            return writer;
        }).when(firstMustache).execute(any(Writer.class), any(Object.class));
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(0);
            writer.write("Hi Alice");
            return writer;
        }).when(secondMustache).execute(any(Writer.class), any(Object.class));

        StepVerifier.create(templateManager.fill("tpl-code", Language.EN, Map.of("name", "Alice")))
                .expectNext("Hello Alice")
                .verifyComplete();

        StepVerifier.create(templateManager.fill("tpl-code", Language.EN, Map.of("name", "Alice")))
                .expectNext("Hi Alice")
                .verifyComplete();

        verify(mustacheFactory, times(2)).compile(any(), anyString());
    }
}
