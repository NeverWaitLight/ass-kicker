package com.github.waitlight.asskicker.channel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FeishuWebhookChannelHandler extends ChannelHandler {

    private final Spec spec;
    private final WebhookChannelSupport webhookSupport;

    public FeishuWebhookChannelHandler(ChannelProviderEntity provider, WebClient webClient) {
        super(webClient);
        this.spec = FeishuSpecMapper.INSTANCE.toSpec(provider.getProperties());
        this.webhookSupport = new WebhookChannelSupport(webClient);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = webhookSupport.normalizeRecipients(uniAddress, "FEISHU");
            String baseUrl = webhookSupport.requireBaseUrl(spec.url(), "FEISHU");

            return Flux.fromIterable(recipients)
                    .concatMap(recipient -> {
                        String endpoint = webhookSupport.buildPathUrl(baseUrl, recipient);
                        byte[] body;
                        try {
                            body = webhookSupport.toJsonBytes(buildPayload(uniMessage));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                        return webhookSupport.postJson(endpoint, body, "FEISHU")
                                .flatMap(this::resolveResponse);
                    })
                    .collect(Collectors.joining(","))
                    .map(ignore -> "FEISHU ok " + recipients.size() + " recipient(s)");
        });
    }

    private Map<String, Object> buildPayload(UniMessage uniMessage) {
        String title = uniMessage != null ? uniMessage.getTitle() : null;
        String content = uniMessage != null ? uniMessage.getContent() : null;
        String text = StringUtils.isNotBlank(title) ? title + "\n" + StringUtils.defaultString(content)
                : StringUtils.defaultString(content);

        Map<String, Object> contentMap = new LinkedHashMap<>();
        contentMap.put("text", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "text");
        payload.put("content", contentMap);
        return payload;
    }

    private Mono<String> resolveResponse(Map<String, Object> response) {
        int code = intValue(response.get("code"), -1);
        if (code != 0) {
            return Mono.error(new IllegalStateException(
                    "FEISHU platform failure code=" + code + " msg=" + String.valueOf(response.get("msg"))));
        }
        return Mono.just("ok");
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.isNumeric(s)) {
            return Integer.parseInt(s);
        }
        return defaultValue;
    }

    record Spec(String url) {
    }
}

@Mapper
interface FeishuSpecMapper {
    FeishuSpecMapper INSTANCE = Mappers.getMapper(FeishuSpecMapper.class);

    @Mapping(target = "url", source = "properties.url")
    FeishuWebhookChannelHandler.Spec toSpec(Map<String, String> properties);
}
