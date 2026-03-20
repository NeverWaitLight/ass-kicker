package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.MsgReq;
import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class WeComIMChannel extends Channel<WeComIMChannelSpec> {

    private final WebClient client;

    public WeComIMChannel(WeComIMChannelSpec spec, WebClient webClient, ChannelDebugProperties debugProperties) {
        super(spec, debugProperties);
        this.client = webClient;
    }

    /**
     * 按 UTF-8 字节截断字符串，满足企业微信「文本内容最长不超过 2048 个字节，必须是 utf8 编码」要求。
     * 从 maxBytes 向前回退，避免在多字节字符中间截断。
     */
    private static String truncateToUtf8Bytes(String s, int maxBytes) {
        if (s == null) {
            return "";
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return s;
        }
        int end = maxBytes;
        while (end > 0 && (bytes[end - 1] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }

    /**
     * 发送企业微信群机器人消息。
     * 官方文档：content 必填，最长不超过 2048 个字节，必须是 UTF-8 编码。
     *
     * @param request 消息请求
     * @return 发送结果
     */
    @Override
    protected MsgResp doSend(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }
        try {
            String webhookUrl = spec.getWebhookUrl();
            String content = buildMessageContent(request);
            content = truncateToUtf8Bytes(content, 2048);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            body.put("text", text);

            WechatWorkResponse response = client
                    .post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(WechatWorkResponse.class)
                    .timeout(spec.getTimeout())
                    .retryWhen(Retry.fixedDelay(spec.getMaxRetries(), spec.getRetryDelay())
                            .filter(this::isRetryableException))
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.error(new RuntimeException(
                                    String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()),
                                    ex)))
                    .onErrorResume(ex -> Mono.error(new RuntimeException(ex.getMessage(), ex)))
                    .block();

            if (response != null && response.getErrcode() == 0) {
                return MsgResp.success(response.getErrmsg() != null ? response.getErrmsg() : "ok");
            } else if (response != null) {
                return MsgResp.failure("WECOM_API_ERROR", response.getErrmsg());
            }
            return MsgResp.failure("WECOM_API_ERROR", "企业微信 API 返回空响应");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private String buildMessageContent(MsgReq request) {
        StringBuilder content = new StringBuilder();
        if (request.subject() != null && !request.subject().isBlank()) {
            content.append("【").append(request.subject()).append("】\n");
        }
        content.append(request.content() != null ? request.content() : "");
        return content.toString();
    }

    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof WebClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            return status == 429 || status == 503 || (status >= 500 && status < 600);
        }
        return ex instanceof ConnectException
                || ex instanceof TimeoutException;
    }

    private String categorizeError(Exception ex) {
        WebClientResponseException responseEx = findCause(ex, WebClientResponseException.class);
        if (responseEx != null) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        }
        if (findCause(ex, ConnectException.class) != null) {
            return "CONNECTION_FAILED";
        }
        if (findCause(ex, TimeoutException.class) != null) {
            return "TIMEOUT";
        }
        return "WECOM_SEND_FAILED";
    }

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
        return "WECOM_SEND_FAILED";
    }

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

    @Setter
    @Getter
    public static class WechatWorkResponse {
        private int errcode;
        private String errmsg;
    }
}
