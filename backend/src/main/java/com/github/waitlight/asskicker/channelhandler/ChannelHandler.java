package com.github.waitlight.asskicker.channelhandler;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;

import reactor.core.publisher.Mono;

public abstract class ChannelHandler {

    public final Mono<String> send(UniMessage uniMessage, UniAddress uniAddress) {
        return doSend(uniMessage, uniAddress);
    }

    protected abstract Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress);
}
