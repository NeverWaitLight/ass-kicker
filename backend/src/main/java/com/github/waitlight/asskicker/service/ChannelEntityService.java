package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChannelEntityService {
    Mono<ChannelEntity> createChannel(ChannelEntity channelEntity);

    Mono<ChannelEntity> getChannelById(String id);

    Flux<ChannelEntity> listChannels();

    Flux<ChannelEntity> findByTypes(List<ChannelType> types);

    Mono<ChannelEntity> updateChannel(String id, ChannelEntity channelEntity);

    Mono<Void> deleteChannel(String id);

    Mono<MsgResp> testSend(TestSendRequest request, UserPrincipal principal);
}
