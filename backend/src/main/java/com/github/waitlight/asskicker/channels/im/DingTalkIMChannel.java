package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.MessageRequest;
import com.github.waitlight.asskicker.channels.MessageResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.HashMap;
import java.util.Map;

public class DingTalkIMChannel extends IMChannel<DingTalkIMChannelConfig> {

    private final WebClient client;

    public DingTalkIMChannel(DingTalkIMChannelConfig config, WebClient webClient) {
        super(config);
        this.client = webClient;
    }

    /**
     * 发送钉钉机器人消息
     *
     * @param request 消息请求
     * @return 发送结果
     */
    @Override
    public MessageResponse send(MessageRequest request) {
        if (request == null) {
            return MessageResponse.failure("INVALID_REQUEST", "Message request is null");
        }
        try {
            // 使用完整的 webhookUrl（已包含 access_token）
            String webhookUrl = config.getWebhookUrl();

            // 构建请求体：{"msgtype": "text", "text": {"content": "xxx"}}
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", buildMessageContent(request));
            body.put("text", text);

            DingTalkResponse response = client
                    .post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(DingTalkResponse.class)
                    .timeout(config.getTimeout())
                    .retryWhen(Retry.fixedDelay(config.getMaxRetries(), config.getRetryDelay())
                            .filter(this::isRetryableException))
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.error(new RuntimeException(
                                    String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()),
                                    ex)))
                    .onErrorResume(ex -> Mono.error(new RuntimeException(ex.getMessage(), ex)))
                    .block();

            if (response != null && response.getErrcode() == 0) {
                return MessageResponse.success(response.getMsgId());
            } else if (response != null) {
                return MessageResponse.failure("DINGTALK_API_ERROR", response.getErrmsg());
            }
            return MessageResponse.failure("DINGTALK_API_ERROR", "钉钉 API 返回空响应");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MessageResponse.failure(errorCode, ex.getMessage());
        }
    }

    /**
     * 构建消息内容
     */
    private String buildMessageContent(MessageRequest request) {
        StringBuilder content = new StringBuilder();

        // 添加主题（如果有）
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            content.append("【").append(request.getSubject()).append("】\n");
        }

        // 添加主要内容
        content.append(request.getContent());

        return content.toString();
    }

    /**
     * 判断是否为可重试的异常
     */
    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof WebClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            // 仅在服务端错误（5xx）和限流（429）时重试，认证错误（401/403）不重试
            return status == 429 || status == 503 || (status >= 500 && status < 600);
        }
        return ex instanceof java.net.ConnectException
                || ex instanceof java.util.concurrent.TimeoutException;
    }

    /**
     * 分类错误码
     */
    private String categorizeError(Exception ex) {
        WebClientResponseException responseEx = findCause(ex, WebClientResponseException.class);
        if (responseEx != null) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        }
        if (findCause(ex, java.net.ConnectException.class) != null) {
            return "CONNECTION_FAILED";
        }
        if (findCause(ex, java.util.concurrent.TimeoutException.class) != null) {
            return "TIMEOUT";
        }
        return "DINGTALK_SEND_FAILED";
    }

    /**
     * 根据 HTTP 状态码分类错误
     */
    private String categorizeHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return "AUTHENTICATION_FAILED";
        } else if (status == 400) {
            return "INVALID_REQUEST";
        } else if (status == 429) {
            return "RATE_LIMIT_EXCEEDED";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "DINGTALK_SEND_FAILED";
    }

    /**
     * 查找异常链中的指定类型异常
     */
    private <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * 钉钉 API 响应类
     */
    @Setter
    @Getter
    public static class DingTalkResponse {
        private int errcode;
        private String errmsg;
        private String msgId;

    }
}
