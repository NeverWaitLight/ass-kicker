package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelService {
    Mono<Channel> createChannel(Channel channel);

    Mono<Channel> getChannelById(Long id);

    Flux<Channel> listChannels();

    Mono<Channel> updateChannel(Long id, Channel channel);

    Mono<Void> deleteChannel(Long id);
}