package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.dto.messagetemplate.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageTemplateService {

    Flux<MessageTemplateEntity> findAll(int page, int size);

    Mono<PageResp<MessageTemplateDTO>> listPage(int page, int size);

    Mono<MessageTemplateEntity> findById(String id);

    Mono<MessageTemplateEntity> findByCode(String code);

    Mono<MessageTemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType);

    Flux<MessageTemplateEntity> findByChannelType(ChannelType channelType);

    Mono<MessageTemplateEntity> create(MessageTemplateEntity entity);

    Mono<MessageTemplateEntity> update(String id, MessageTemplateEntity entity);

    Mono<Void> delete(String id);
}
