package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ChannelService;
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
public class ChannelHandler {

    private final ChannelService channelService;
    private final TestSendService testSendService;
    private final TestSendProperties testSendProperties;
    private static final String CHANNEL_TYPE_ERROR = "通道类型必须为SMS、EMAIL、IM、PUSH";
    private static final String TEST_SEND_FAILED_MESSAGE = "测试发送失败";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public ChannelHandler(ChannelService channelService,
                          TestSendService testSendService,
                          TestSendProperties testSendProperties) {
        this.channelService = channelService;
        this.testSendService = testSendService;
        this.testSendProperties = testSendProperties;
    }

    public Mono<ServerResponse> createChannel(ServerRequest request) {
        return request.bodyToMono(Channel.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CHANNEL_TYPE_ERROR))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CHANNEL_TYPE_ERROR))
                .onErrorMap(IllegalArgumentException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()))
                .map(this::sanitizeChannel)
                .flatMap(this::validateChannel)
                .flatMap(channelService::createChannel)
                .flatMap(channel -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(channel))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "创建通道失败" : ex.getReason()));
    }

    public Mono<ServerResponse> listChannels(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(channelService.listChannels(), Channel.class)
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取通道列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getChannelById(ServerRequest request) {
        String id = request.pathVariable("id");
        return channelService.getChannelById(id)
                .flatMap(channel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(channel))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取通道失败" : ex.getReason()));
    }

    public Mono<ServerResponse> updateChannel(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(Channel.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CHANNEL_TYPE_ERROR))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CHANNEL_TYPE_ERROR))
                .onErrorMap(IllegalArgumentException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()))
                .map(this::sanitizeChannel)
                .flatMap(this::validateChannel)
                .flatMap(body -> channelService.updateChannel(id, body))
                .flatMap(channel -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(channel))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "更新通道失败" : ex.getReason()));
    }

    public Mono<ServerResponse> deleteChannel(ServerRequest request) {
        String id = request.pathVariable("id");
        return channelService.deleteChannel(id)
                .then(ServerResponse.noContent().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "删除通道失败" : ex.getReason()));
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
                                ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CHANNEL_TYPE_ERROR))
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

    public Mono<ServerResponse> listChannelTypes(ServerRequest request) {
        List<String> types = Arrays.stream(ChannelType.values())
                .map(ChannelType::name)
                .toList();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(types);
    }

    private Mono<Channel> validateChannel(Channel channel) {
        if (channel == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "通道数据不能为空"));
        }
        if (channel.getName() == null || channel.getName().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "通道名称不能为空"));
        }
        if (channel.getType() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "通道类型不能为空"));
        }
        if (channel.getProperties() == null) {
            channel.setProperties(new LinkedHashMap<>());
        }
        validateProperties(channel.getProperties(), "properties");
        return Mono.just(channel);
    }

    private void validateProperties(Map<String, Object> properties, String path) {
        if (properties == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通道属性键不能为空");
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

    private Channel sanitizeChannel(Channel input) {
        Channel sanitized = new Channel();
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择通道类型");
        }
        String target = sanitizeText(input.target());
        if (target.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标地址不能为空");
        }
        if (target.length() > testSendProperties.getMaxTargetLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标地址过长");
        }
        if (input.type() == ChannelType.EMAIL && !EMAIL_PATTERN.matcher(target).matches()) {
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
