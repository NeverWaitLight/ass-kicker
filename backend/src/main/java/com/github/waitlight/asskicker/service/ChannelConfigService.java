package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.model.ChannelConfig;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChannelConfigService {
    Mono<ChannelConfig> createChannel(ChannelConfig channelConfig);

    Mono<ChannelConfig> getChannelById(String id);

    Flux<ChannelConfig> listChannels();

    Flux<ChannelConfig> findByTypes(List<ChannelType> types);

    Mono<ChannelConfig> updateChannel(String id, ChannelConfig channelConfig);

    Mono<Void> deleteChannel(String id);

    Mono<MsgResp> testSend(TestSendRequest request, UserPrincipal principal);
}
