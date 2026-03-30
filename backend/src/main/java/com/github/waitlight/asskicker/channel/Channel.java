package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public abstract class Channel {

    protected final WebClient webClient;

    protected Channel(WebClient webClient) {
        this.webClient = webClient;
    }

    public final Mono<String> send(UniMessage uniMessage, UniAddress uniAddress) {
        return doSend(uniMessage, uniAddress);
    }

    protected abstract Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress);
}
