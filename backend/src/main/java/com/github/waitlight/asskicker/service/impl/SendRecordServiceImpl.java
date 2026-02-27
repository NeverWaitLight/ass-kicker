package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.dto.sendrecord.SendRecordPageResponse;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordView;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import com.github.waitlight.asskicker.service.SendRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class SendRecordServiceImpl implements SendRecordService {

    private final SendRecordRepository sendRecordRepository;

    public SendRecordServiceImpl(SendRecordRepository sendRecordRepository) {
        this.sendRecordRepository = sendRecordRepository;
    }

    @Override
    public Mono<SendRecordPageResponse> listRecords(int page, int size, String recipient, String channelType) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int offset = (normalizedPage - 1) * normalizedSize;
        String recipientFilter = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        String channelTypeFilter = (channelType != null && !channelType.isBlank()) ? channelType.trim() : null;

        Mono<Long> totalMono = sendRecordRepository.countAll(recipientFilter, channelTypeFilter);
        Mono<List<SendRecordView>> itemsMono = sendRecordRepository.findPage(normalizedSize, offset, recipientFilter, channelTypeFilter)
                .map(this::toView)
                .collectList();

        return Mono.zip(itemsMono, totalMono)
                .map(tuple -> new SendRecordPageResponse(tuple.getT1(), normalizedPage, normalizedSize, tuple.getT2()));
    }

    @Override
    public Mono<SendRecordView> getById(String id) {
        return sendRecordRepository.findById(id)
                .map(this::toView)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "发送记录不存在")));
    }

    private SendRecordView toView(SendRecord r) {
        return new SendRecordView(
                r.getId(),
                r.getTaskId(),
                r.getTemplateCode(),
                r.getLanguageCode(),
                r.getParams(),
                r.getChannelId(),
                r.getRecipients(),
                r.getRecipient(),
                r.getSubmittedAt(),
                r.getRenderedContent(),
                r.getChannelType(),
                r.getChannelName(),
                r.getStatus(),
                r.getErrorCode(),
                r.getErrorMessage(),
                r.getSentAt()
        );
    }
}
