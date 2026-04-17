package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import com.github.waitlight.asskicker.model.ChannelEntity;
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
import com.github.waitlight.asskicker.service.ChannelService;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ChannelManagerRefreshFailureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ChannelService channelService;

    @Mock
    private ChannelFactory channelFactory;

    private ChannelManager channelManager;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        channelManager = new ChannelManager(channelService, channelFactory, validator, MAPPER);
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
        ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
        Channel channel = new NoOpChannel(entity, WebClient.create(),
                ChannelTestObjectMappers.channelObjectMapper());
        ConcurrentHashMap<String, Channel> map = new ConcurrentHashMap<>();
        map.put("id-1", channel);
        ReflectionTestUtils.setField(channelManager, "cache", map);

        when(channelService.findEnabled()).thenReturn(Flux.error(new RuntimeException("db unavailable")));

        channelManager.refresh();

        assertThat(channelManager.getChannelCount()).isEqualTo(1);
    }

    /** Avoids MapStruct-backed channels so this test does not depend on generated mapper classes. */
    private static final class NoOpChannel extends Channel {

        NoOpChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
            super(entity, webClient, objectMapper);
        }

        @Override
        protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
            return Mono.empty();
        }
    }
}
