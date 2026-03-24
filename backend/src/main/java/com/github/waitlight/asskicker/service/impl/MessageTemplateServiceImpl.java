package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.repository.MessageTemplateRepository;
import com.github.waitlight.asskicker.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final MessageTemplateRepository messageTemplateRepository;
    private final MessageTemplateConverter messageTemplateConverter;

    @Override
    public Flux<MessageTemplateEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return messageTemplateRepository.findAll()
                .skip(offset)
                .take(size);
    }

    @Override
    public Mono<MessageTemplateEntity> findById(String id) {
        return messageTemplateRepository.findById(id);
    }

    @Override
    public Mono<MessageTemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType) {
        return messageTemplateRepository.findByCodeAndChannelType(code, channelType);
    }

    @Override
    public Flux<MessageTemplateEntity> findByChannelType(ChannelType channelType) {
        return messageTemplateRepository.findByChannelType(channelType);
    }

    @Override
    public Mono<MessageTemplateEntity> create(MessageTemplateEntity entity) {
        MessageTemplateEntity toCreate = messageTemplateConverter.copyForCreate(entity);
        toCreate.setId(null);
        long now = Instant.now().toEpochMilli();
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        return ensureUniqueCodeAndType(toCreate.getCode(), toCreate.getChannelType(), null)
                .then(Mono.defer(() -> messageTemplateRepository.save(toCreate)));
    }

    @Override
    public Mono<MessageTemplateEntity> update(String id, MessageTemplateEntity entity) {
        return messageTemplateRepository.findById(id)
                .flatMap(existing -> ensureUniqueCodeAndType(entity.getCode(), entity.getChannelType(), id)
                        .then(Mono.defer(() -> {
                            messageTemplateConverter.merge(entity, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return messageTemplateRepository.save(existing);
                        })));
    }

    @Override
    public Mono<Void> delete(String id) {
        return messageTemplateRepository.deleteById(id);
    }

    private Mono<Void> ensureUniqueCodeAndType(String code, ChannelType channelType, String currentId) {
        return messageTemplateRepository.findByCodeAndChannelType(code, channelType)
                .flatMap(existing -> {
                    if (currentId != null && currentId.equals(existing.getId())) {
                        return Mono.empty();
                    }
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Message template already exists: " + code + " / " + channelType));
                })
                .then();
    }
}
