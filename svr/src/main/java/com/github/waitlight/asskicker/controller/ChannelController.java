package com.github.waitlight.asskicker.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.converter.ChannelConverter;
import com.github.waitlight.asskicker.converter.ChannelPropertiesMapper;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.dto.channel.ChannelTestResultVO;
import com.github.waitlight.asskicker.dto.channel.CreateChannelDTO;
import com.github.waitlight.asskicker.dto.channel.TestChannelDTO;
import com.github.waitlight.asskicker.dto.channel.ChannelPropertiesSchemaVO;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.dto.channel.ChannelVO;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.dto.channel.UpdateChannelDTO;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.service.RecordService;
import com.github.waitlight.asskicker.model.RecordEntity;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import org.bson.types.ObjectId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.time.Instant;

@Tag(name = "ChannelController")
@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
@Validated
public class ChannelController {

        private final ChannelService channelService;
        private final ChannelConverter channelConverter;
        private final ChannelManager channelManager;
        private final ChannelFactory channelFactory;
        private final ChannelPropertiesMapper channelPropertiesMapper;
        private final RecordService recordService;

        @Operation(summary = "test", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PostMapping("/test")
        public Mono<Resp<ChannelTestResultVO>> test(@Valid @RequestBody TestChannelDTO request) {
                ProviderType provider = resolveProviderForTest(request.getType(), request.getProvider());
                Map<String, Object> props = request.getProperties() != null ? request.getProperties() : Map.of();
                channelManager.validateProperties(provider, props);

                ChannelEntity ephemeral = new ChannelEntity();
                ephemeral.setId("0");
                ephemeral.setCode("test");
                ephemeral.setName("test");
                ephemeral.setChannelType(request.getType());
                ephemeral.setProviderType(provider);
                ephemeral.setEnabled(true);
                ephemeral.setProperties(channelPropertiesMapper.channelObjectPropertiesToProperties(props));

                Channel channel = channelFactory.create(ephemeral);
                if (channel == null) {
                        return Mono.just(Resp.success(ChannelTestResultVO.fail("unsupported provider")));
                }

                UniMessage message = new UniMessage();
                message.setContent(request.getContent());

                UniAddress address = buildTestAddress(request.getType(), provider, request.getTarget().trim(),
                                ephemeral.getCode());

                long submittedAt = Instant.now().toEpochMilli();
                String taskId = ObjectId.get().toString();
                UniTask task = UniTask.builder().message(message).address(address).taskId(taskId).submittedAt(submittedAt)
                                .build();

                return channel.send(task)
                                .flatMap(ignore -> {
                                        writeChannelTestRecord(ephemeral, task, request.getTarget().trim(),
                                                        message.getContent(), true, null);
                                        return Mono.just(Resp.success(ChannelTestResultVO.ok()));
                                })
                                .onErrorResume(e -> {
                                        writeChannelTestRecord(ephemeral, task, request.getTarget().trim(),
                                                        message.getContent(), false, e.getMessage());
                                        return Mono.just(Resp.success(ChannelTestResultVO.fail(e.getMessage())));
                                });
        }

        private void writeChannelTestRecord(ChannelEntity channelEntity, UniTask task, String recipient,
                        String renderedContent, boolean success, String errorMessage) {
                RecordEntity sr = new RecordEntity();
                sr.setTaskId(task.getTaskId());
                UniMessage msg = task.getMessage();
                if (msg != null) {
                        sr.setTemplateCode(msg.getTemplateCode());
                        if (msg.getLanguage() != null) {
                                sr.setLanguageCode(msg.getLanguage().getCode());
                        }
                        sr.setParams(msg.getTemplateParams());
                }
                sr.setRecipient(recipient);
                sr.setChannelId(channelEntity.getId());
                sr.setChannelType(channelEntity.getChannelType());
                sr.setChannelName(channelEntity.getCode());
                sr.setSubmittedAt(task.getSubmittedAt() != null ? task.getSubmittedAt()
                                : System.currentTimeMillis());
                sr.setRenderedContent(renderedContent);
                if (success) {
                        sr.setStatus(SendRecordStatus.SUCCESS);
                        sr.setSentAt(System.currentTimeMillis());
                } else {
                        sr.setStatus(SendRecordStatus.FAILED);
                        sr.setErrorMessage(errorMessage);
                }
                recordService.writeRecord(sr);
        }

        @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PostMapping
        public Mono<Resp<ChannelVO>> create(@Valid @RequestBody CreateChannelDTO request) {
                channelManager.validateProperties(request.getProvider(), request.getProperties());

                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(channelService::create)
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping
        public Mono<PageResp<ChannelVO>> page(@Validated PageReq pageReq,
                        @RequestParam(required = false) ChannelType channelType,
                        @RequestParam(required = false) ProviderType providerType) {
                int page = pageReq.getPage();
                int size = pageReq.getSize();
                String keyword = pageReq.getKeyword();
                int offset = (page - 1) * size;

                return channelService.count(keyword, channelType, providerType)
                                .flatMap(total -> {
                                        if (total == 0) {
                                                return Mono.just(PageResp.success(page, size, total, List.of()));
                                        }
                                        return channelService.list(keyword, channelType, providerType, size, offset)
                                                        .map(channelConverter::toVO)
                                                        .collectList()
                                                        .map(channels -> PageResp.success(page, size, total, channels));
                                });
        }

        @Operation(summary = "channelTypes", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/types")
        public Mono<Resp<List<ChannelType>>> channelTypes() {
                return Mono.fromSupplier(() -> List.of(ChannelType.values()))
                                .map(Resp::success);
        }

        @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{id}")
        public Mono<Resp<ChannelVO>> getById(@PathVariable String id) {
                return channelService.getById(id)
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "providerProperties", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{providerType}/properties")
        public Mono<Resp<ChannelPropertiesSchemaVO>> providerProperties(@PathVariable ProviderType providerType) {
                return Mono.fromSupplier(() -> ChannelPropertiesSchemaVO.builder()
                                .properties(channelManager.getPropertiesSchema(providerType))
                                .build())
                                .map(Resp::success);
        }

        @Operation(summary = "providersByChannelType", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{channelType}/providers")
        public Mono<Resp<List<ChannelProviderOptionVO>>> providersByChannelType(
                        @PathVariable ChannelType channelType) {
                return Mono.fromSupplier(() -> channelManager.getProvidersByChannelType(channelType))
                                .map(Resp::success);
        }

        @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PutMapping
        public Mono<Resp<ChannelVO>> update(@Valid @RequestBody UpdateChannelDTO request) {
                channelManager.validateProperties(request.getProvider(), request.getProperties());

                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(patch -> channelService.update(request.getId(), patch))
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @DeleteMapping("/{id}")
        public Mono<Void> delete(@PathVariable String id) {
                return channelService.delete(id);
        }

        private static ProviderType resolveProviderForTest(ChannelType channelType, ProviderType explicit) {
                if (explicit != null) {
                        if (explicit.getChannelType() != channelType) {
                                throw new BadRequestException("provider does not match channel type");
                        }
                        return explicit;
                }
                List<ProviderType> candidates = Arrays.stream(ProviderType.values())
                                .filter(p -> p.getChannelType() == channelType)
                                .toList();
                if (candidates.size() == 1) {
                        return candidates.get(0);
                }
                throw new BadRequestException("provider is required when multiple providers exist for this channel type");
        }

        private static UniAddress buildTestAddress(ChannelType channelType, ProviderType provider, String target,
                        String channelCode) {
                return switch (channelType) {
                        case EMAIL -> UniAddress.ofEmail(target);
                        case SMS -> UniAddress.ofSms(target);
                        case PUSH -> UniAddress.ofPush(provider, target);
                        case IM -> {
                                if (provider.name().endsWith("_WEBHOOK")) {
                                        yield UniAddress.ofImWebhook(provider, target);
                                }
                                yield UniAddress.ofImBot(provider, channelCode, target);
                        }
                };
        }
}