package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.service.MessageTemplateService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessageTemplateHandler {

    private final MessageTemplateService messageTemplateService;
    private final MessageTemplateConverter messageTemplateConverter;
    private final Validator validator;

    public Mono<ServerResponse> listPage(ServerRequest request) {
        int page = parseInt(request.queryParam("page").orElse("1"), 1);
        int size = parseInt(request.queryParam("size").orElse("10"), 10);
        return messageTemplateService.listPage(page, size)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "获取消息模板列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(MessageTemplateDTO.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .switchIfEmpty(Mono
                        .error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空")))
                .flatMap(this::validateDto)
                .map(messageTemplateConverter::toEntity)
                .flatMap(messageTemplateService::create)
                .map(messageTemplateConverter::toDto)
                .flatMap(dto -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "创建消息模板失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        return messageTemplateService.findById(id)
                .map(messageTemplateConverter::toDto)
                .flatMap(dto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "获取消息模板失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getByCode(ServerRequest request) {
        String code = request.pathVariable("code");
        return messageTemplateService.findByCode(code)
                .map(messageTemplateConverter::toDto)
                .flatMap(dto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "获取消息模板失败" : ex.getReason()));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(MessageTemplateDTO.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .switchIfEmpty(Mono
                        .error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空")))
                .flatMap(this::validateDto)
                .map(messageTemplateConverter::toEntity)
                .flatMap(entity -> messageTemplateService.update(id, entity))
                .map(messageTemplateConverter::toDto)
                .flatMap(dto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "更新消息模板失败" : ex.getReason()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        return messageTemplateService.delete(id)
                .then(ServerResponse.noContent().build())
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "删除消息模板失败" : ex.getReason()));
    }

    private Mono<MessageTemplateDTO> validateDto(MessageTemplateDTO dto) {
        Set<ConstraintViolation<MessageTemplateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
        }
        return Mono.just(dto);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
