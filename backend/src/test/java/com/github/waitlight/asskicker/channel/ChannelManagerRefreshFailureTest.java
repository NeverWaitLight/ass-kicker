package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.service.ChannelProviderService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ChannelManagerRefreshFailureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ChannelProviderService channelProviderService;

    @Mock
    private ChannelFactory channelFactory;

    private ChannelManager channelManager;

    @BeforeEach
    void setUp() {
        channelManager = new ChannelManager(channelProviderService, channelFactory);
    }

    @Test
    void refresh_failure_keepsExistingCache() throws Exception {
        String json = """
                {
                  "id": "id-1",
                  "code": "dummy-refresh",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "enabled": true
                }
                """;
        ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
        Channel channel = new NoOpChannel(entity, WebClient.create());
        ConcurrentHashMap<String, Channel> map = new ConcurrentHashMap<>();
        map.put("id-1", channel);
        ReflectionTestUtils.setField(channelManager, "cache", map);

        when(channelProviderService.findEnabled()).thenReturn(Flux.error(new RuntimeException("db unavailable")));

        channelManager.refresh();

        assertThat(channelManager.getChannelCount()).isEqualTo(1);
    }

    /** Avoids MapStruct-backed channels so this test does not depend on generated mapper classes. */
    private static final class NoOpChannel extends Channel {

        NoOpChannel(ChannelProviderEntity entity, WebClient webClient) {
            super(entity, webClient);
        }

        @Override
        protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
            return Mono.empty();
        }
    }
}
