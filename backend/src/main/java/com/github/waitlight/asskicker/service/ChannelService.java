package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChannelService {
    Mono<Channel> createChannel(Channel channel);

    Mono<Channel> getChannelById(String id);

    Flux<Channel> listChannels();

    Flux<Channel> findByTypes(List<ChannelType> types);

    Mono<Channel> updateChannel(String id, Channel channel);

    Mono<Void> deleteChannel(String id);

    Mono<MsgResp> testSend(TestSendRequest request, UserPrincipal principal);
}
