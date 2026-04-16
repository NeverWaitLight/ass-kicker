package com.github.waitlight.asskicker.channel;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ChannelImpl(providerType = ProviderType.AWS_SMS, propertyClass = AwsSnsSmsChannel.Properties.class)
public class AwsSnsSmsChannel extends Channel {

    private final Properties properties;
    private final SnsAsyncClient snsClient;

    public AwsSnsSmsChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.snsClient = buildSnsClient();
    }

    private SnsAsyncClient buildSnsClient() {
        String accessKeyId = properties.accessKeyId().trim();
        String secret = properties.secretAccessKey().trim();
        AwsCredentialsProvider credentialsProvider;
        if (StringUtils.isNotBlank(properties.sessionToken())) {
            credentialsProvider = StaticCredentialsProvider.create(AwsSessionCredentials.create(
                    accessKeyId,
                    secret,
                    properties.sessionToken().trim()));
        } else {
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secret));
        }
        var builder = SnsAsyncClient.builder()
                .region(Region.of(properties.region().trim()))
                .credentialsProvider(credentialsProvider);
        if (StringUtils.isNotBlank(properties.endpoint())) {
            builder.endpointOverride(endpointOverrideUri(properties.endpoint().trim()));
        }
        return builder.build();
    }

    private static URI endpointOverrideUri(String raw) {
        String s = raw.trim();
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return URI.create(s);
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

            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(phone, messageText))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "AWS_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(properties.accessKeyId()) || StringUtils.isBlank(properties.secretAccessKey())
                || StringUtils.isBlank(properties.region())) {
            throw new IllegalStateException("AWS_SMS requires accessKeyId secretAccessKey region");
        }
    }

    private Mono<String> sendOne(String phoneNumber, String messageText) {
        PublishRequest request = PublishRequest.builder()
                .phoneNumber(phoneNumber.trim())
                .message(messageText)
                .build();
        return Mono.fromFuture(snsClient.publish(request))
                .map(response -> {
                    if (response.messageId() == null || response.messageId().isBlank()) {
                        throw new IllegalStateException("AWS_SMS publish returned empty messageId");
                    }
                    return "ok";
                })
                .onErrorMap(AwsSnsSmsChannel::mapAwsFailure);
    }

    private static Throwable mapAwsFailure(Throwable t) {
        if (t instanceof AwsServiceException ase) {
            String msg = ase.awsErrorDetails() != null && StringUtils.isNotBlank(ase.awsErrorDetails().errorMessage())
                    ? ase.awsErrorDetails().errorMessage()
                    : ase.getMessage();
            return new IllegalStateException("AWS_SMS " + ase.statusCode() + ": " + msg, t);
        }
        return new IllegalStateException("AWS_SMS request failed", t);
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

    record Properties(
            @NotBlank(message = "accessKeyId 不能为空") String accessKeyId,
            @NotBlank(message = "secretAccessKey 不能为空") String secretAccessKey,
            @NotBlank(message = "region 不能为空") String region,
            String sessionToken,
            String endpoint) {
    }
}
