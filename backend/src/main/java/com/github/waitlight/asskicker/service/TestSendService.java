package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.channel.TestSendRequest;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.sender.MessageResponse;
import reactor.core.publisher.Mono;

public interface TestSendService {
    Mono<MessageResponse> testSend(TestSendRequest request, UserPrincipal principal);
}