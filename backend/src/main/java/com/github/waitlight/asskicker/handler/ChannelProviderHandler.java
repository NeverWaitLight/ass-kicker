package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.channelprovider.ChannelProviderDTO;
import com.github.waitlight.asskicker.service.ChannelProviderService;
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
public class ChannelProviderHandler {

        private final ChannelProviderService channelProviderService;
        private final ChannelProviderConverter channelProviderConverter;
        private final Validator validator;

        public Mono<ServerResponse> create(ServerRequest request) {
                return request.bodyToMono(ChannelProviderDTO.class)
                                .onErrorMap(ServerWebInputException.class,
                                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                                .onErrorMap(DecodingException.class,
                                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                                .switchIfEmpty(Mono
                                                .error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空")))
                                .flatMap(this::validateDto)
                                .map(channelProviderConverter::toEntity)
                                .flatMap(channelProviderService::create)
                                .map(channelProviderConverter::toDto)
                                .flatMap(dto -> ServerResponse.status(HttpStatus.CREATED)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(dto))
                                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                                                .status(ex.getStatusCode())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ex.getReason() == null ? "创建通道商失败" : ex.getReason()));
        }

        public Mono<ServerResponse> listPage(ServerRequest request) {
                int page = parseInt(request.queryParam("page").orElse("1"), 1);
                int size = parseInt(request.queryParam("size").orElse("10"), 10);
                return channelProviderService.listPage(page, size)
                                .flatMap(response -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(response))
                                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                                                .status(ex.getStatusCode())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ex.getReason() == null ? "获取通道商列表失败" : ex.getReason()));
        }

        public Mono<ServerResponse> getById(ServerRequest request) {
                String id = request.pathVariable("id");
                return channelProviderService.findById(id)
                                .map(channelProviderConverter::toDto)
                                .flatMap(dto -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(dto))
                                .switchIfEmpty(ServerResponse.notFound().build())
                                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                                                .status(ex.getStatusCode())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ex.getReason() == null ? "获取通道商失败" : ex.getReason()));
        }

        public Mono<ServerResponse> update(ServerRequest request) {
                String id = request.pathVariable("id");
                return request.bodyToMono(ChannelProviderDTO.class)
                                .onErrorMap(ServerWebInputException.class,
                                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                                .onErrorMap(DecodingException.class,
                                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                                .switchIfEmpty(Mono
                                                .error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空")))
                                .flatMap(this::validateDto)
                                .map(channelProviderConverter::toEntity)
                                .flatMap(patch -> channelProviderService.update(id, patch))
                                .map(channelProviderConverter::toDto)
                                .flatMap(dto -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(dto))
                                .switchIfEmpty(ServerResponse.notFound().build())
                                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                                                .status(ex.getStatusCode())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ex.getReason() == null ? "更新通道商失败" : ex.getReason()));
        }

        public Mono<ServerResponse> delete(ServerRequest request) {
                String id = request.pathVariable("id");
                return channelProviderService.delete(id)
                                .then(ServerResponse.noContent().build())
                                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                                                .status(ex.getStatusCode())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ex.getReason() == null ? "删除通道商失败" : ex.getReason()));
        }

        private Mono<ChannelProviderDTO> validateDto(ChannelProviderDTO dto) {
                Set<ConstraintViolation<ChannelProviderDTO>> violations = validator.validate(dto);
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
