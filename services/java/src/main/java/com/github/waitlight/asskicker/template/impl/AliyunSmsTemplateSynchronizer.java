package com.github.waitlight.asskicker.template.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.AddSmsTemplateRequest;
import com.aliyun.dysmsapi20170525.models.AddSmsTemplateResponse;
import com.aliyun.dysmsapi20170525.models.AddSmsTemplateResponseBody;
import com.aliyun.dysmsapi20170525.models.ModifySmsTemplateRequest;
import com.aliyun.dysmsapi20170525.models.ModifySmsTemplateResponse;
import com.aliyun.dysmsapi20170525.models.ModifySmsTemplateResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.impl.AliyunSmsChannel;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.template.AbstractTemplateSynchronizer;
import com.github.waitlight.asskicker.template.SyncContext;
import com.github.waitlight.asskicker.template.SyncResult;
import com.github.waitlight.asskicker.template.TemplateSync;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@TemplateSync(type = ChannelType.SMS, provider = ChannelProvider.ALIYUN)
public class AliyunSmsTemplateSynchronizer extends AbstractTemplateSynchronizer {

    private static final String DEFAULT_ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String SUCCESS_CODE = "OK";
    private static final int DEFAULT_TEMPLATE_TYPE = 1;

    private final ObjectMapper channelObjectMapper;

    public AliyunSmsTemplateSynchronizer(
            @Qualifier(ChannelObjectMapperConfig.BEAN_NAME) ObjectMapper channelObjectMapper) {
        this.channelObjectMapper = channelObjectMapper;
    }

    @Override
    public Mono<SyncResult> sync(SyncContext context) {
        return Mono.fromCallable(() -> doSync(context))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SyncResult doSync(SyncContext context) {
        ChannelEntity channel = context.channel();
        AliyunSmsChannel.Properties props = channelObjectMapper
                .convertValue(channel.getProperties(), AliyunSmsChannel.Properties.class);
        Client client;
        try {
            client = new Client(new Config()
                    .setAccessKeyId(props.getAccessKeyId())
                    .setAccessKeySecret(props.getAccessKeySecret())
                    .setEndpoint(StringUtils.defaultIfBlank(props.getEndpoint(), DEFAULT_ENDPOINT)));
        } catch (Exception e) {
            throw new SendException("template.sync.client.init.failed: " + e.getMessage(), e);
        }

        String templateName = resolveTemplateName(context);
        String templateContent = context.localized().getContent();
        Integer templateType = context.smsTemplateType() != null ? context.smsTemplateType() : DEFAULT_TEMPLATE_TYPE;
        String remark = StringUtils.defaultIfBlank(context.remark(), context.template().getName());
        String existingCode = context.existingProviderTemplateCode();

        try {
            if (StringUtils.isBlank(existingCode)) {
                AddSmsTemplateRequest request = new AddSmsTemplateRequest()
                        .setTemplateName(templateName)
                        .setTemplateContent(templateContent)
                        .setTemplateType(templateType)
                        .setRemark(remark);
                AddSmsTemplateResponse response = client.addSmsTemplate(request);
                AddSmsTemplateResponseBody body = response == null ? null : response.getBody();
                if (body == null || !StringUtils.equalsIgnoreCase(SUCCESS_CODE, body.getCode())) {
                    throw new SendException(body == null ? "empty response" : (body.getCode() + ": " + body.getMessage()));
                }
                if (StringUtils.isBlank(body.getTemplateCode())) {
                    throw new SendException("template.sync.aliyun.templateCode.empty");
                }
                return new SyncResult(body.getTemplateCode());
            }

            ModifySmsTemplateRequest request = new ModifySmsTemplateRequest()
                    .setTemplateCode(existingCode)
                    .setTemplateName(templateName)
                    .setTemplateContent(templateContent)
                    .setTemplateType(templateType)
                    .setRemark(remark);
            ModifySmsTemplateResponse response = client.modifySmsTemplate(request);
            ModifySmsTemplateResponseBody body = response == null ? null : response.getBody();
            if (body == null || !StringUtils.equalsIgnoreCase(SUCCESS_CODE, body.getCode())) {
                throw new SendException(body == null ? "empty response" : (body.getCode() + ": " + body.getMessage()));
            }
            return new SyncResult(existingCode);
        } catch (SendException e) {
            throw e;
        } catch (Exception e) {
            throw new SendException(e.getMessage(), e);
        }
    }

    private String resolveTemplateName(SyncContext context) {
        String title = context.localized().getTitle();
        if (StringUtils.isNotBlank(title)) {
            return trimTo(title, 30);
        }
        return trimTo(context.template().getName(), 30);
    }

    private String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
