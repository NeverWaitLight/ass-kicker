package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.repository.MessageTemplateRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageTemplateService {

    private final MessageTemplateRepository messageTemplateRepository;
    private final MessageTemplateConverter messageTemplateConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public Flux<MessageTemplateEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return messageTemplateRepository.findAll()
                .skip(offset)
                .take(size);
    }

    public Mono<PageResp<MessageTemplateDTO>> page(int page, int size) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int zeroBasedPage = normalizedPage - 1;
        Mono<Long> totalMono = messageTemplateRepository.count();
        Mono<List<MessageTemplateDTO>> itemsMono =
                findAll(zeroBasedPage, normalizedSize)
                        .map(messageTemplateConverter::toDto)
                        .collectList();
        return Mono.zip(itemsMono, totalMono)
                .map(t -> PageResp.success(normalizedPage, normalizedSize, t.getT2(), t.getT1()));
    }

    public Mono<MessageTemplateEntity> findById(String id) {
        return messageTemplateRepository.findById(id);
    }

    public Mono<MessageTemplateEntity> findByCode(String code) {
        return messageTemplateRepository.findByCode(code);
    }

    public Mono<MessageTemplateEntity> findByCodeAndChannelType(String code, ChannelType channelType) {
        return messageTemplateRepository.findByCodeAndChannelType(code, channelType);
    }

    public Flux<MessageTemplateEntity> findByChannelType(ChannelType channelType) {
        return messageTemplateRepository.findByChannelType(channelType);
    }

    public Mono<MessageTemplateEntity> create(MessageTemplateEntity entity) {
        MessageTemplateEntity toCreate = messageTemplateConverter.copyForCreate(entity);
        toCreate.setId(null);
        long now = Instant.now().toEpochMilli();
        toCreate.setId(snowflakeIdGenerator.nextIdString());
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        return ensureUniqueCode(toCreate.getCode(), null)
                .then(Mono.defer(() -> messageTemplateRepository.save(toCreate)));
    }

    public Mono<MessageTemplateEntity> update(String id, MessageTemplateEntity entity) {
        return messageTemplateRepository.findById(id)
                .flatMap(existing -> ensureUniqueCode(entity.getCode(), id)
                        .then(Mono.defer(() -> {
                            messageTemplateConverter.merge(entity, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return messageTemplateRepository.save(existing);
                        })));
    }

    public Mono<Void> delete(String id) {
        return messageTemplateRepository.deleteById(id);
    }

    private Mono<Void> ensureUniqueCode(String code, String currentId) {
        return messageTemplateRepository.findByCode(code)
                .flatMap(existing -> {
                    if (currentId != null && currentId.equals(existing.getId())) {
                        return Mono.empty();
                    }
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Message template already exists: " + code));
                })
                .then();
    }
}
