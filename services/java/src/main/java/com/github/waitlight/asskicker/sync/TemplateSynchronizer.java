package com.github.waitlight.asskicker.sync;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import reactor.core.publisher.Mono;

/**
 * 将本地 LocalizedTemplateEntity 同步（新建或更新）至服务商。
 * 每个 (ChannelType, ChannelProvider) 提供独立实现，并通过 {@link TemplateSync} 注解声明能力。
 */
public interface TemplateSynchronizer {

    ChannelType type();

    ChannelProvider provider();

    /**
     * 执行同步操作，返回服务商侧的模板 code / templateId。
     * 实现方需要根据 context.existingProviderTemplateCode 决定走 create 还是 update。
     */
    Mono<SyncResult> sync(SyncContext context);
}
