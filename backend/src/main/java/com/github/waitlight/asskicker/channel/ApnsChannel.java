package com.github.waitlight.asskicker.channel;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@ChannelImpl(providerType = ProviderType.APNS, propertyClass = ApnsChannel.Properties.class)
public class ApnsChannel extends Channel {

    private final Properties properties;

    public ApnsChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = ApnsSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                    ? List.of()
                    : uniAddress.getRecipients().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toList());

            if (recipients.isEmpty()) {
                return Mono.error(new IllegalArgumentException("APNs recipients required"));
            }

            if (StringUtils.isBlank(properties.url())
                    || StringUtils.isBlank(properties.bundleIdTopic())
                    || StringUtils.isBlank(properties.teamId())
                    || StringUtils.isBlank(properties.keyId())
                    || StringUtils.isBlank(properties.privateKeyPem())) {
                return Mono.error(new IllegalStateException(
                        "APNs spec requires url bundleIdTopic teamId keyId privateKeyPem"));
            }

            UUID parsedApnsId = properties.apnsId() == null || properties.apnsId().isBlank()
                    ? null
                    : UUID.fromString(properties.apnsId().trim());
            String alertTitle = uniMessage != null ? uniMessage.getTitle() : null;
            String alertBody = uniMessage != null ? uniMessage.getContent() : null;
            Map<String, Object> extraData = uniMessage != null ? uniMessage.getExtraData() : null;

            log.info("Sending APNs notification to {} recipient(s)", recipients.size());

            return Mono.fromCallable(() -> {
                PrivateKey key = loadEcPrivateKeyFromPem(properties.privateKeyPem());
                return buildApnsJwt(key, properties.teamId().trim(), properties.keyId().trim());
            })
                    .flatMapMany(jwt -> {
                        byte[] bodyBytes;
                        try {
                            bodyBytes = buildApnsPayloadBytes(objectMapper, alertTitle, alertBody, extraData);
                        } catch (Exception e) {
                            return Flux.error(e);
                        }
                        String endpointBase = normalizeApnsEndpointBase(properties.url());
                        return Flux.fromIterable(recipients)
                                .concatMap(token -> postApnsDevice(
                                        jwt,
                                        endpointBase,
                                        token,
                                        properties.bundleIdTopic().trim(),
                                        parsedApnsId,
                                        bodyBytes));
                    })
                    .collect(Collectors.joining(","))
                    .map(ids -> "APNs ok " + recipients.size() + " device(s) apns-id=" + ids);
        });
    }

    private static PrivateKey loadEcPrivateKeyFromPem(String pem) throws Exception {
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static String buildApnsJwt(PrivateKey key, String teamId, String keyId) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setIssuer(teamId)
                .setIssuedAt(new Date(nowSeconds * 1000))
                .setExpiration(new Date((nowSeconds + 3600) * 1000))
                .signWith(key, SignatureAlgorithm.ES256)
                .compact();
    }

    private static byte[] buildApnsPayloadBytes(ObjectMapper objectMapper, String title, String body,
            Map<String, Object> extraData)
            throws Exception {
        Map<String, Object> alert = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(title)) {
            alert.put("title", title);
        }
        alert.put("body", body != null ? body : "");
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", alert);
        aps.put("sound", "default");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("aps", aps);
        if (extraData != null && !extraData.isEmpty()) {
            for (Map.Entry<String, Object> e : extraData.entrySet()) {
                String k = e.getKey();
                if (k != null && !k.isBlank() && !"aps".equals(k)) {
                    payload.put(k, e.getValue());
                }
            }
        }
        return objectMapper.writeValueAsBytes(payload);
    }

    private static String normalizeApnsEndpointBase(String url) {
        String u = url.trim();
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private Mono<String> postApnsDevice(
            String jwt,
            String endpointBase,
            String deviceToken,
            String bundleIdTopic,
            UUID apnsId,
            byte[] bodyBytes) {
        String uri = endpointBase + "/" + deviceToken.trim();
        return webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "bearer " + jwt)
                .header("apns-topic", bundleIdTopic)
                .header("apns-push-type", "alert")
                .header("apns-priority", "10")
                .headers(h -> {
                    if (apnsId != null) {
                        h.set("apns-id", apnsId.toString());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyBytes)
                .retrieve()
                .toBodilessEntity()
                .map(entity -> Optional.ofNullable(entity.getHeaders().getFirst("apns-id")).orElse("ok"))
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        "APNs " + ex.getStatusCode().value()
                                + (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isBlank()
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    record Properties(
            @NotBlank(message = "url 不能为空") String url,
            @NotBlank(message = "bundleIdTopic 不能为空") String bundleIdTopic,
            @NotBlank(message = "teamId 不能为空") String teamId,
            @NotBlank(message = "keyId 不能为空") String keyId,
            @NotBlank(message = "privateKeyPem 不能为空") String privateKeyPem,
            String apnsId) {
    }
}

@Mapper
interface ApnsSpecMapper {
    ApnsSpecMapper INSTANCE = Mappers.getMapper(ApnsSpecMapper.class);

    @Mapping(target = "url", source = "properties.url")
    @Mapping(target = "bundleIdTopic", source = "properties.bundleIdTopic")
    @Mapping(target = "teamId", source = "properties.teamId")
    @Mapping(target = "keyId", source = "properties.keyId")
    @Mapping(target = "privateKeyPem", source = "properties.privateKeyPem")
    @Mapping(target = "apnsId", source = "properties.apnsId")
    ApnsChannel.Properties toSpec(Map<String, String> properties);
}
