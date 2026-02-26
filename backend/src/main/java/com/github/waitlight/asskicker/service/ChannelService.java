package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelService {
    Mono<Channel> createChannel(Channel channel);

    Mono<Channel> getChannelById(String id);

    Flux<Channel> listChannels();

    Mono<Channel> updateChannel(String id, Channel channel);

    Mono<Void> deleteChannel(String id);
}
