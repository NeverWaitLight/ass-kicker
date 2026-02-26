package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.dto.sender.TestSendRequest;
import com.github.waitlight.asskicker.model.Sender;
import com.github.waitlight.asskicker.model.SenderType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.SenderService;
import com.github.waitlight.asskicker.service.TestSendService;
import com.github.waitlight.asskicker.testsend.TestSendProperties;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SenderHandler {

    private final SenderService senderService;
    private final TestSendService testSendService;
    private final TestSendProperties testSendProperties;
    private static final String SENDER_TYPE_ERROR = "发送端类型必须为SMS、EMAIL、IM、PUSH";
    private static final String TEST_SEND_FAILED_MESSAGE = "测试发送失败";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public SenderHandler(SenderService senderService,
                         TestSendService testSendService,
                         TestSendProperties testSendProperties) {
        this.senderService = senderService;
        this.testSendService = testSendService;
        this.testSendProperties = testSendProperties;
    }

    public Mono<ServerResponse> createSender(ServerRequest request) {
        return request.bodyToMono(Sender.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, SENDER_TYPE_ERROR))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, SENDER_TYPE_ERROR))
                .onErrorMap(IllegalArgumentException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()))
                .map(this::sanitizeSender)
                .flatMap(this::validateSender)
                .flatMap(senderService::createSender)
                .flatMap(sender -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(sender))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "创建发送端失败" : ex.getReason()));
    }

    public Mono<ServerResponse> listSenders(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(senderService.listSenders(), Sender.class)
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取发送端列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getSenderById(ServerRequest request) {
        String id = request.pathVariable("id");
        return senderService.getSenderById(id)
                .flatMap(sender -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(sender))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取发送端失败" : ex.getReason()));
    }

    public Mono<ServerResponse> updateSender(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(Sender.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, SENDER_TYPE_ERROR))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, SENDER_TYPE_ERROR))
                .onErrorMap(IllegalArgumentException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()))
                .map(this::sanitizeSender)
                .flatMap(this::validateSender)
                .flatMap(body -> senderService.updateSender(id, body))
                .flatMap(sender -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(sender))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "更新发送端失败" : ex.getReason()));
    }

    public Mono<ServerResponse> deleteSender(ServerRequest request) {
        String id = request.pathVariable("id");
        return senderService.deleteSender(id)
                .then(ServerResponse.noContent().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "删除发送端失败" : ex.getReason()));
    }

    public Mono<ServerResponse> testSend(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未授权")))
                .flatMap(principal -> request.bodyToMono(TestSendRequest.class)
                        .onErrorMap(ServerWebInputException.class,
                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                        .onErrorMap(DecodingException.class,
                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, SENDER_TYPE_ERROR))
                        .onErrorMap(IllegalArgumentException.class,
                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()))
                        .map(this::sanitizeTestSend)
                        .flatMap(body -> testSendService.testSend(body, principal)))
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? TEST_SEND_FAILED_MESSAGE : ex.getReason()));
    }

    public Mono<ServerResponse> listSenderTypes(ServerRequest request) {
        List<String> types = Arrays.stream(SenderType.values())
                .map(SenderType::name)
                .toList();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(types);
    }

    private Mono<Sender> validateSender(Sender sender) {
        if (sender == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送端数据不能为空"));
        }
        if (sender.getName() == null || sender.getName().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送端名称不能为空"));
        }
        if (sender.getType() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送端类型不能为空"));
        }
        if (sender.getProperties() == null) {
            sender.setProperties(new LinkedHashMap<>());
        }
        validateProperties(sender.getProperties(), "properties");
        return Mono.just(sender);
    }

    private void validateProperties(Map<String, Object> properties, String path) {
        if (properties == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送端属性键不能为空");
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                validateProperties(castMap(mapValue), path + "." + key);
            } else if (value instanceof List<?> listValue) {
                validateList(listValue, path + "." + key);
            }
        }
    }

    private void validateList(List<?> list, String path) {
        int index = 0;
        for (Object value : list) {
            String currentPath = path + "[" + index + "]";
            if (value instanceof Map<?, ?> mapValue) {
                validateProperties(castMap(mapValue), currentPath);
            } else if (value instanceof List<?> listValue) {
                validateList(listValue, currentPath);
            }
            index++;
        }
    }

    private Map<String, Object> castMap(Map<?, ?> value) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Sender sanitizeSender(Sender input) {
        Sender sanitized = new Sender();
        if (input == null) {
            return sanitized;
        }
        sanitized.setName(input.getName());
        sanitized.setType(input.getType());
        sanitized.setDescription(input.getDescription());
        sanitized.setProperties(input.getProperties());
        return sanitized;
    }

    private TestSendRequest sanitizeTestSend(TestSendRequest input) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "测试数据不能为空");
        }
        if (input.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择发送端类型");
        }
        String target = sanitizeText(input.target());
        if (target.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标地址不能为空");
        }
        if (target.length() > testSendProperties.getMaxTargetLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标地址过长");
        }
        if (input.type() == SenderType.EMAIL && !EMAIL_PATTERN.matcher(target).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标地址格式不正确");
        }
        String content = sanitizeText(input.content());
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "测试内容不能为空");
        }
        if (content.length() > testSendProperties.getMaxContentLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "测试内容过长");
        }
        Map<String, Object> properties = normalizeTestProperties(input.properties());
        validateProperties(properties, "properties");
        return new TestSendRequest(input.type(), properties, target, content);
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return filterControlChars(trimmed).trim();
    }

    private String filterControlChars(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!Character.isISOControl(ch) || ch == '\n' || ch == '\r' || ch == '\t') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> normalizeTestProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return castMap(properties);
    }
}
