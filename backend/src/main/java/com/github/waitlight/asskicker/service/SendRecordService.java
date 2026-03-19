package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.sendrecord.SendRecordPageResponse;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordView;
import com.github.waitlight.asskicker.model.SendRecordEntity;
import reactor.core.publisher.Mono;

public interface SendRecordService {

    Mono<SendRecordPageResponse> listRecords(int page, int size, String recipient, String channelType);

    Mono<SendRecordView> getById(String id);

    void writeRecord(SendRecordEntity record);
}
