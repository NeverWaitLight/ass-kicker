package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.ChannelCryptoProperties;
import com.github.waitlight.asskicker.channel.ChannelPropertyCrypto;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.service.impl.ChannelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    private ChannelServiceImpl channelService;

    @BeforeEach
    void setUp() {
        ChannelCryptoProperties properties = new ChannelCryptoProperties();
        properties.setSecret("test-channel-secret");
        properties.setSensitiveKeys(java.util.List.of("apikey", "password", "secret"));
        ChannelPropertyCrypto propertyCrypto = new ChannelPropertyCrypto(properties);
        channelService = new ChannelServiceImpl(channelRepository, propertyCrypto, new ObjectMapper());
    }

    @Test
    void shouldEncryptSensitivePropertiesOnCreate() {
        Channel input = new Channel();
        input.setName("Email");
        input.setType(ChannelType.EMAIL);
        input.setDescription("desc");
        input.setProperties(new LinkedHashMap<>(Map.of("apiKey", "plain-secret", "host", "smtp")));

        when(channelRepository.save(any(Channel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(channelService.createChannel(input))
                .assertNext(saved -> {
                    assertEquals("Email", saved.getName());
                    assertEquals(ChannelType.EMAIL, saved.getType());
                    assertNotNull(saved.getCreatedAt());
                    assertEquals("plain-secret", saved.getProperties().get("apiKey"));
                    assertEquals("smtp", saved.getProperties().get("host"));
                })
                .verifyComplete();

        ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
        verify(channelRepository).save(captor.capture());
        Channel persisted = captor.getValue();
        assertNotNull(persisted.getPropertiesJson());
        assertTrue(persisted.getPropertiesJson().contains("enc:"));
    }

    @Test
    void shouldDecryptSensitivePropertiesOnRead() {
        Channel stored = new Channel();
        stored.setId(1L);
        stored.setName("SMS");
        stored.setType(ChannelType.SMS);
        stored.setDescription("desc");

        Channel input = new Channel();
        input.setName("SMS");
        input.setType(ChannelType.SMS);
        input.setDescription("desc");
        input.setProperties(new LinkedHashMap<>(Map.of("password", "secret", "sender", "svc")));

        when(channelRepository.save(any(Channel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Channel saved = channelService.createChannel(input).block();
        assertNotNull(saved);
        stored.setPropertiesJson(saved.getPropertiesJson());
        stored.setCreatedAt(saved.getCreatedAt());
        stored.setUpdatedAt(saved.getUpdatedAt());

        when(channelRepository.findAll()).thenReturn(Flux.just(stored));

        StepVerifier.create(channelService.listChannels())
                .assertNext(channel -> {
                    assertEquals("secret", channel.getProperties().get("password"));
                    assertEquals("svc", channel.getProperties().get("sender"));
                })
                .verifyComplete();
    }
}
