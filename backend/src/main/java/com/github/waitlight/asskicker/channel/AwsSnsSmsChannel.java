package com.github.waitlight.asskicker.channel;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.aws.AwsV4Signer;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AwsSnsSmsChannel extends Channel {

    private static final Pattern ERROR_CODE = Pattern.compile("<Code>\\s*([^<]+)\\s*</Code>");
    private static final Pattern ERROR_MSG = Pattern.compile("<Message>\\s*([^<]+)\\s*</Message>");

    private final Spec spec;

    public AwsSnsSmsChannel(ChannelProviderEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = AwsSnsSmsSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress, "AWS_SMS");
            validateSpec();

            String messageText = uniMessage != null ? StringUtils.defaultString(uniMessage.getContent()) : "";
            if (StringUtils.isBlank(messageText)) {
                return Mono.error(new IllegalArgumentException("AWS_SMS message content required"));
            }

            String region = spec.region().trim();
            Instant signingInstant = Instant.now();

            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(region, phone, messageText, signingInstant))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "AWS_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(spec.accessKeyId()) || StringUtils.isBlank(spec.secretAccessKey())
                || StringUtils.isBlank(spec.region())) {
            throw new IllegalStateException("AWS_SMS requires accessKeyId secretAccessKey region");
        }
    }

    private Mono<String> sendOne(String region, String phoneNumber, String messageText, Instant signingInstant) {
        AwsV4Signer.SignedRequest signed = AwsV4Signer.signSnsPublish(
                region,
                spec.endpoint(),
                spec.accessKeyId().trim(),
                spec.secretAccessKey().trim(),
                spec.sessionToken(),
                phoneNumber.trim(),
                messageText,
                signingInstant);

        return webClient.post()
                .uri(signed.url())
                .headers(h -> {
                    for (Map.Entry<String, String> e : signed.headers().entrySet()) {
                        h.add(e.getKey(), e.getValue());
                    }
                })
                .bodyValue(signed.body().getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseSnsResponse)
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        "AWS_SMS " + ex.getStatusCode().value()
                                + (StringUtils.isNotBlank(ex.getResponseBodyAsString())
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    private Mono<String> parseSnsResponse(String xml) {
        if (StringUtils.isBlank(xml)) {
            return Mono.error(new IllegalStateException("AWS_SMS empty response"));
        }
        if (xml.contains("<Error>")) {
            String code = firstMatch(ERROR_CODE, xml);
            String msg = firstMatch(ERROR_MSG, xml);
            return Mono.error(new IllegalStateException(
                    "AWS_SMS failure " + StringUtils.defaultString(code) + ": " + StringUtils.defaultString(msg)));
        }
        if (xml.contains("<MessageId>")) {
            return Mono.just("ok");
        }
        return Mono.error(new IllegalStateException("AWS_SMS unexpected response: " + xml));
    }

    private static String firstMatch(Pattern p, String xml) {
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : "";
    }

    private List<String> normalizeRecipients(UniAddress uniAddress, String providerName) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(providerName + " recipients required");
        }
        return recipients;
    }

    record Spec(String accessKeyId, String secretAccessKey, String region, String sessionToken, String endpoint) {
    }
}

@Mapper
interface AwsSnsSmsSpecMapper {
    AwsSnsSmsSpecMapper INSTANCE = Mappers.getMapper(AwsSnsSmsSpecMapper.class);

    @Mapping(target = "accessKeyId", source = "properties.accessKeyId")
    @Mapping(target = "secretAccessKey", source = "properties.secretAccessKey")
    @Mapping(target = "region", source = "properties.region")
    @Mapping(target = "sessionToken", source = "properties.sessionToken")
    @Mapping(target = "endpoint", source = "properties.endpoint")
    AwsSnsSmsChannel.Spec toSpec(Map<String, String> properties);
}
