package com.github.waitlight.asskicker.exception;

import com.github.waitlight.asskicker.dto.Resp;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 对象绑定与方法参数两类校验失败统一走 i18n 模板查找。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class BindExceptionHandler {

    /**
     * 命中则走 {@code validation.{Annotation}} 通用模板，否则按 {@code {messageKey}} 原样查表。
     */
    private static final Set<String> COMMON_VALIDATION_ANNOTATIONS = Set.of(
            "NotBlank", "NotNull", "NotEmpty", "Min", "Max", "Pattern", "Size", "Email");

    private final MessageSource messageSource;

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Resp<Void>>> handleWebExchangeBindException(
            WebExchangeBindException ex, Locale locale) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> resolveLocalizedMessage(
                        error.getCode(),
                        buildArgs(error.getField(), error),
                        error.getDefaultMessage(),
                        error.getDefaultMessage(),
                        locale))
                .orElse(null);

        log.warn("Validation exception: message={}", message);
        return Mono.just(ResponseEntity.badRequest()
                .body(new Resp<>("BAD_REQUEST", message, null)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<Resp<Void>>> handleConstraintViolationException(
            ConstraintViolationException ex, Locale locale) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> {
                    String annotation = violation.getConstraintDescriptor()
                            .getAnnotation().annotationType().getSimpleName();
                    String field = resolveLeafName(violation.getPropertyPath());
                    Object[] args = buildArgs(field, annotation,
                            violation.getConstraintDescriptor().getAttributes());
                    return resolveLocalizedMessage(annotation, args,
                            violation.getMessageTemplate(), violation.getMessage(), locale);
                })
                .orElse(null);

        log.warn("Constraint violation exception: message={}", message);
        return Mono.just(ResponseEntity.badRequest()
                .body(new Resp<>("BAD_REQUEST", message, null)));
    }

    /**
     * 命中通用注解走 {@code validation.{Annotation}}；否则若模板形如 {@code {key}} 则去花括号查表；都不命中返回 fallback。
     */
    private String resolveLocalizedMessage(String annotation, Object[] args,
                                           String rawTemplate, String fallbackMessage, Locale locale) {
        if (annotation != null && COMMON_VALIDATION_ANNOTATIONS.contains(annotation)) {
            return messageSource.getMessage("validation." + annotation, args, fallbackMessage, locale);
        }
        if (rawTemplate != null && rawTemplate.startsWith("{") && rawTemplate.endsWith("}")) {
            return messageSource.getMessage(rawTemplate.substring(1, rawTemplate.length() - 1),
                    null, fallbackMessage, locale);
        }
        return fallbackMessage;
    }

    /**
     * 把 Spring 放在 args[0] 的字段对象替换为 fieldPath，并将后续 {@link MessageSourceResolvable} 拍平成字符串。
     */
    private Object[] buildArgs(String fieldPath, FieldError error) {
        Object[] raw = error.getArguments();
        if (raw == null || raw.length <= 1) {
            return new Object[]{fieldPath};
        }
        Object[] args = new Object[raw.length];
        args[0] = fieldPath;
        for (int i = 1; i < raw.length; i++) {
            Object value = raw[i];
            args[i] = value instanceof MessageSourceResolvable resolvable
                    ? String.join(",", resolvable.getCodes() == null ? new String[0] : resolvable.getCodes())
                    : value;
        }
        return args;
    }

    /**
     * 占位符顺序与 {@link #buildArgs(String, FieldError)} 对齐，便于复用同一套 {@code validation.{Annotation}} 模板。
     */
    private Object[] buildArgs(String fieldPath, String annotation, Map<String, Object> attributes) {
        return switch (annotation) {
            case "Min", "Max" -> new Object[]{fieldPath, attributes.get("value")};
            case "Size" -> new Object[]{fieldPath, attributes.get("max"), attributes.get("min")};
            case "Pattern" -> new Object[]{fieldPath, attributes.get("regexp")};
            default -> new Object[]{fieldPath};
        };
    }

    /**
     * 取 propertyPath 末段作为字段名，规避方法前缀与返回值标记。
     */
    private String resolveLeafName(Path path) {
        String leaf = null;
        for (Path.Node node : path) {
            leaf = node.getName();
        }
        return leaf;
    }
}
