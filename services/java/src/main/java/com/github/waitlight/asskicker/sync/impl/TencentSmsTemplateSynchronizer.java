package com.github.waitlight.asskicker.sync.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.impl.TencentSmsChannel;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.sync.AbstractTemplateSynchronizer;
import com.github.waitlight.asskicker.sync.SyncContext;
import com.github.waitlight.asskicker.sync.SyncResult;
import com.github.waitlight.asskicker.sync.TemplateSync;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.AddSmsTemplateRequest;
import com.tencentcloudapi.sms.v20210111.models.AddSmsTemplateResponse;
import com.tencentcloudapi.sms.v20210111.models.AddTemplateStatus;
import com.tencentcloudapi.sms.v20210111.models.ModifySmsTemplateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@TemplateSync(type = ChannelType.SMS, provider = ChannelProvider.TENCENT)
public class TencentSmsTemplateSynchronizer extends AbstractTemplateSynchronizer {

    private static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String DEFAULT_REGION = "ap-guangzhou";
    private static final long DEFAULT_SMS_TYPE = 1L;

    private final ObjectMapper channelObjectMapper;

    public TencentSmsTemplateSynchronizer(
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
        TencentSmsChannel.Properties props = channelObjectMapper
                .convertValue(channel.getProperties(), TencentSmsChannel.Properties.class);
        SmsClient client;
        try {
            Credential credential = new Credential(props.getSecretId(), props.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(StringUtils.defaultIfBlank(props.getEndpoint(), DEFAULT_ENDPOINT));
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            client = new SmsClient(credential,
                    StringUtils.defaultIfBlank(props.getRegion(), DEFAULT_REGION),
                    clientProfile);
        } catch (Exception e) {
            throw new SendException("template.sync.client.init.failed: " + e.getMessage(), e);
        }

        String templateName = resolveTemplateName(context);
        String templateContent = context.localized().getContent();
        long smsType = context.smsTemplateType() != null ? context.smsTemplateType() : DEFAULT_SMS_TYPE;
        long international = Boolean.TRUE.equals(context.international()) ? 1L : 0L;
        String remark = StringUtils.defaultIfBlank(context.remark(), context.template().getName());
        String existingCode = context.existingProviderTemplateCode();

        try {
            if (StringUtils.isBlank(existingCode)) {
                AddSmsTemplateRequest req = new AddSmsTemplateRequest();
                req.setTemplateName(templateName);
                req.setTemplateContent(templateContent);
                req.setSmsType(smsType);
                req.setInternational(international);
                req.setRemark(remark);
                AddSmsTemplateResponse resp = client.AddSmsTemplate(req);
                AddTemplateStatus status = resp == null ? null : resp.getAddTemplateStatus();
                if (status == null || StringUtils.isBlank(status.getTemplateId())) {
                    throw new SendException("template.sync.tencent.templateId.empty");
                }
                return new SyncResult(status.getTemplateId());
            }

            long templateId;
            try {
                templateId = Long.parseLong(existingCode);
            } catch (NumberFormatException e) {
                throw new SendException("template.sync.tencent.templateId.invalid: " + existingCode);
            }
            ModifySmsTemplateRequest req = new ModifySmsTemplateRequest();
            req.setTemplateId(templateId);
            req.setTemplateName(templateName);
            req.setTemplateContent(templateContent);
            req.setSmsType(smsType);
            req.setInternational(international);
            req.setRemark(remark);
            client.ModifySmsTemplate(req);
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
