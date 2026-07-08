package com.github.waitlight.asskicker.channel.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.RecordService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Channel(type = ChannelType.FCM, provider = ChannelProvider.GOOGLE, reqType = PushReq.class)
public class FcmPushChannel extends AbstractChannel<PushReq> {

    private final Properties properties;
    private final FirebaseApp firebaseApp;
    private final FirebaseMessaging messaging;

    public FcmPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
                          RecordService recordService) {
        super(provider, webClient, objectMapper, recordService);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        validateSpec(this.properties);
        this.firebaseApp = buildFirebaseApp(provider, this.properties);
        this.messaging = FirebaseMessaging.getInstance(this.firebaseApp);
    }

    FcmPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
                   RecordService recordService, FirebaseApp firebaseApp, FirebaseMessaging messaging) {
        super(provider, webClient, objectMapper, recordService);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.firebaseApp = firebaseApp;
        this.messaging = messaging;
    }

    public static FcmPushChannel forTesting(ChannelEntity provider, WebClient webClient,
            ObjectMapper objectMapper, RecordService recordService, FirebaseMessaging messaging) {
        return new FcmPushChannel(provider, webClient, objectMapper, recordService, null, messaging);
    }

    @Override
    protected Mono<String> doSend(PushReq req) {
        return Mono.defer(() -> {
            String token = StringUtils.trimToNull(req.getDeviceToken());
            if (token == null) {
                return Mono.error(new IllegalArgumentException("FCM deviceToken required"));
            }
            Message message = buildMessage(token, req);
            log.info("Sending FCM notification to device token ***{}",
                    token.length() > 6 ? token.substring(token.length() - 6) : token);
            return Mono.fromCallable(() -> messaging.send(message))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(name -> "FCM ok name=" + name)
                    .onErrorMap(FirebaseMessagingException.class, ex -> new IllegalStateException(
                            "FCM " + ex.getMessagingErrorCode() + ": " + ex.getMessage(), ex));
        });
    }

    @Override
    public void dispose() {
        if (firebaseApp != null) {
            try {
                firebaseApp.delete();
            } catch (Exception e) {
                log.warn("FCM FirebaseApp delete failed", e);
            }
        }
    }

    private static Message buildMessage(String deviceToken, PushReq req) {
        Notification.Builder notification = Notification.builder()
                .setBody(req.getBody() != null ? req.getBody() : "");
        if (StringUtils.isNotBlank(req.getTitle())) {
            notification.setTitle(req.getTitle());
        }

        AndroidNotification.Builder androidNotification = AndroidNotification.builder()
                .setBody(req.getBody() != null ? req.getBody() : "");
        if (StringUtils.isNotBlank(req.getTitle())) {
            androidNotification.setTitle(req.getTitle());
        }

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(resolveAndroidPriority(req.getPriority()))
                .setNotification(androidNotification.build())
                .build();

        Message.Builder builder = Message.builder()
                .setToken(deviceToken)
                .setNotification(notification.build())
                .setAndroidConfig(androidConfig);

        Map<String, Object> data = req.getData();
        if (data != null && !data.isEmpty()) {
            Map<String, String> dataMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                String k = e.getKey();
                if (k != null && !k.isBlank()) {
                    dataMap.put(k, e.getValue() != null ? e.getValue().toString() : "");
                }
            }
            if (!dataMap.isEmpty()) {
                builder.putAllData(dataMap);
            }
        }
        return builder.build();
    }

    private static AndroidConfig.Priority resolveAndroidPriority(String value) {
        if (StringUtils.isBlank(value)) {
            return AndroidConfig.Priority.HIGH;
        }
        try {
            return AndroidConfig.Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "FCM unsupported priority: " + value + " (allowed: NORMAL, HIGH)", ex);
        }
    }

    private static void validateSpec(Properties p) {
        if (StringUtils.isBlank(p.getProjectId()) || StringUtils.isBlank(p.getServiceAccountJson())) {
            throw new IllegalStateException("FCM spec requires projectId serviceAccountJson");
        }
    }

    private static FirebaseApp buildFirebaseApp(ChannelEntity entity, Properties p) {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(p.getServiceAccountJson().getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(p.getProjectId().trim())
                    .build();
            String base = StringUtils.isNotBlank(entity.getId()) ? entity.getId() : "fcm";
            String appName = "fcm-" + base + "-" + UUID.randomUUID();
            return FirebaseApp.initializeApp(options, appName);
        } catch (IOException e) {
            throw new IllegalStateException("FCM credentials init failed", e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Properties {

        /** Firebase 项目 ID,对应 Firebase 控制台的 project id */
        @NotBlank
        private String projectId;

        /** Firebase 服务账号 JSON 密钥文件的完整内容(字符串形式),用于获取 OAuth2 access token */
        @NotBlank
        private String serviceAccountJson;
    }

}
