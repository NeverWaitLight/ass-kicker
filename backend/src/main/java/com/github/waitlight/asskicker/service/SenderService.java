package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Sender;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SenderService {
    Mono<Sender> createSender(Sender sender);

    Mono<Sender> getSenderById(String id);

    Flux<Sender> listSenders();

    Mono<Sender> updateSender(String id, Sender sender);

    Mono<Void> deleteSender(String id);
}
