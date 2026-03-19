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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class DingTalkIMChannel extends Channel<DingTalkIMChannelSpec> {

    private final WebClient client;

    public DingTalkIMChannel(DingTalkIMChannelSpec spec, WebClient webClient, ChannelDebugProperties debugProperties) {
        super(spec, debugProperties);
        this.client = webClient;
    }

    /**
     * 发送钉钉消息
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
            // 使用完整的 webhookUrl（已包含 access_token）
            String webhookUrl = spec.getWebhookUrl();

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
                return MsgResp.success(response.getMsgId());
            } else if (response != null) {
                return MsgResp.failure("DINGTALK_API_ERROR", response.getErrmsg());
            }
            return MsgResp.failure("DINGTALK_API_ERROR", "钉钉 API 返回空响应");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    /**
     * 构建消息内容
     */
    private String buildMessageContent(MsgReq request) {
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
        return ex instanceof ConnectException
                || ex instanceof TimeoutException;
    }

    /**
     * 分类错误码
     */
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
