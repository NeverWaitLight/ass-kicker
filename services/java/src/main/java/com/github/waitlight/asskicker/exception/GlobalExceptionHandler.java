package com.github.waitlight.asskicker.exception;

import com.github.waitlight.asskicker.dto.Resp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<Resp<Void>>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Resp<>("NOT_FOUND", "Resource not found", null)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Resp<Void>>> handleException(Exception ex, Locale locale) {
        log.error("Unexpected exception: ", ex);
        String message = messageSource.getMessage("server.error", null, "Internal server error", locale);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Resp<>("INTERNAL_ERROR", message, null)));
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<Resp<Void>>> handleBusinessException(BusinessException ex, Locale locale) {
        String message = ex.getMessageKey() != null
                ? messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessageKey(), locale)
                : ex.getMessage();

        Resp<Void> errorResponse = new Resp<>(ex.getCode(), message, null);
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

}