package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import com.github.waitlight.asskicker.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private LanguageTemplateRepository languageTemplateRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private Template template;
    private Template template2;

    @BeforeEach
    void setUp() {
        template = new Template();
        template.setId(1L);
        template.setName("Test Template");
        template.setCode("test-code");
        template.setDescription("desc");
        template.setCreatedAt(1L);
        template.setUpdatedAt(1L);

        template2 = new Template();
        template2.setId(2L);
        template2.setName("Another Template");
        template2.setCode("another-code");
        template2.setDescription("desc2");
        template2.setCreatedAt(2L);
        template2.setUpdatedAt(2L);
    }

    @Test
    void shouldFindAllTemplates() {
        when(templateRepository.findAll()).thenReturn(Flux.just(template, template2));

        StepVerifier.create(templateService.findAll(0, 1))
                .expectNextMatches(result -> "Test Template".equals(result.getName()))
                .verifyComplete();

        verify(templateRepository, times(1)).findAll();
    }

    @Test
    void shouldFindTemplateById() {
        when(templateRepository.findById(1L)).thenReturn(Mono.just(template));

        StepVerifier.create(templateService.findById(1L))
                .expectNextMatches(result -> "Test Template".equals(result.getName()))
                .verifyComplete();

        verify(templateRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenTemplateNotFoundById() {
        when(templateRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(templateService.findById(1L))
                .verifyComplete();

        verify(templateRepository, times(1)).findById(1L);
    }

    @Test
    void shouldFindTemplateByCode() {
        when(templateRepository.findByCode("test-code")).thenReturn(Mono.just(template));

        StepVerifier.create(templateService.findByCode("test-code"))
                .expectNextMatches(result -> "test-code".equals(result.getCode()))
                .verifyComplete();

        verify(templateRepository, times(1)).findByCode("test-code");
    }

    @Test
    void shouldCreateTemplateSetsTimestamps() {
        when(templateRepository.save(any(Template.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(templateService.createTemplate(template))
                .assertNext(result -> {
                    assertEquals("Test Template", result.getName());
                    assertNotNull(result.getCreatedAt());
                    assertNotNull(result.getUpdatedAt());
                })
                .verifyComplete();

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository, times(1)).save(captor.capture());
        Template saved = captor.getValue();
        assertEquals("Test Template", saved.getName());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateTemplate() {
        Template incoming = new Template();
        incoming.setName("Updated Name");
        incoming.setCode("updated-code");
        incoming.setDescription("updated-desc");

        when(templateRepository.findById(1L)).thenReturn(Mono.just(template));
        when(templateRepository.save(any(Template.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(templateService.updateTemplate("1", incoming))
                .assertNext(result -> {
                    assertEquals("Updated Name", result.getName());
                    assertEquals("updated-code", result.getCode());
                    assertEquals("updated-desc", result.getDescription());
                    assertNotNull(result.getUpdatedAt());
                })
                .verifyComplete();

        verify(templateRepository, times(1)).findById(1L);
        verify(templateRepository, times(1)).save(any(Template.class));
    }

    @Test
    void shouldDeleteTemplateById() {
        when(templateRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(templateService.deleteTemplate("1"))
                .verifyComplete();

        verify(templateRepository, times(1)).deleteById(1L);
    }
}
