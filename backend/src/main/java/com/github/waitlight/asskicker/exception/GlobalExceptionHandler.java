package com.github.waitlight.asskicker.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex, Locale locale) {
        log.error("Unexpected exception: ", ex);
        String message = messageSource.getMessage("server.error", null, "Internal server error", locale);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", message)));
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(BusinessException ex, Locale locale) {
        String message = ex.getMessageKey() != null
                ? messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessageKey(), locale)
                : ex.getMessage();

        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), message);
        HttpStatus httpStatus = switch (ex.getCode()) {
            case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "PERMISSION_DENIED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        log.warn("Business exception: code={}, message={}", ex.getCode(), message);
        return Mono.just(ResponseEntity.status(httpStatus).body(errorResponse));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex,
            Locale locale) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String defaultMessage = error.getDefaultMessage();
                    if (defaultMessage != null && defaultMessage.startsWith("{") && defaultMessage.endsWith("}")) {
                        return messageSource.getMessage(defaultMessage.substring(1, defaultMessage.length() - 1), null,
                                defaultMessage, locale);
                    }
                    return defaultMessage;
                })
                .distinct()
                .collect(Collectors.joining("; "));

        log.warn("Validation exception: message={}", message);
        return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex,
            Locale locale) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        log.warn("Constraint violation exception: message={}", message);
        return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message)));
    }

    public record ErrorResponse(String code, String message) {
    }
}