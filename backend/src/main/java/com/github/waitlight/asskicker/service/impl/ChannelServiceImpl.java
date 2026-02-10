package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.ChannelPropertyCrypto;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.service.ChannelService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ChannelServiceImpl implements ChannelService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };

    private final ChannelRepository channelRepository;
    private final ChannelPropertyCrypto propertyCrypto;
    private final ObjectMapper objectMapper;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              ChannelPropertyCrypto propertyCrypto,
                              ObjectMapper objectMapper) {
        this.channelRepository = channelRepository;
        this.propertyCrypto = propertyCrypto;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Channel> createChannel(Channel channel) {
        Channel toSave = new Channel();
        toSave.setId(null);
        toSave.setName(channel.getName());
        toSave.setType(channel.getType());
        toSave.setDescription(channel.getDescription());
        long timestamp = Instant.now().toEpochMilli();
        toSave.setCreatedAt(timestamp);
        toSave.setUpdatedAt(timestamp);
        Map<String, Object> properties = normalizeProperties(channel.getProperties());
        Map<String, Object> encrypted = propertyCrypto.encryptSensitive(properties);
        toSave.setPropertiesJson(writeProperties(encrypted));
        return channelRepository.save(toSave)
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Channel> getChannelById(Long id) {
        return channelRepository.findById(id)
                .map(this::enrichChannel);
    }

    @Override
    public Flux<Channel> listChannels() {
        return channelRepository.findAll()
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Channel> updateChannel(Long id, Channel channel) {
        return channelRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(channel.getName());
                    existing.setType(channel.getType());
                    existing.setDescription(channel.getDescription());
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    Map<String, Object> properties = normalizeProperties(channel.getProperties());
                    Map<String, Object> encrypted = propertyCrypto.encryptSensitive(properties);
                    existing.setPropertiesJson(writeProperties(encrypted));
                    return channelRepository.save(existing);
                })
                .map(this::enrichChannel);
    }

    @Override
    public Mono<Void> deleteChannel(Long id) {
        return channelRepository.deleteById(id);
    }

    private Channel enrichChannel(Channel channel) {
        Map<String, Object> properties = readProperties(channel.getPropertiesJson());
        Map<String, Object> decrypted = propertyCrypto.decryptSensitive(properties);
        channel.setProperties(decrypted);
        return channel;
    }

    private Map<String, Object> normalizeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(properties);
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            System.err.println("Failed to read channel properties: " + ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String writeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception ex) {
            System.err.println("Failed to write channel properties: " + ex.getMessage());
            return "{}";
        }
    }
}
