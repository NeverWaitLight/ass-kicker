package com.github.waitlight.asskicker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.Sender;
import com.github.waitlight.asskicker.repository.SenderRepository;
import com.github.waitlight.asskicker.sendercrypto.SenderPropertyCrypto;
import com.github.waitlight.asskicker.service.SenderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SenderServiceImpl implements SenderService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };

    private final SenderRepository senderRepository;
    private final SenderPropertyCrypto propertyCrypto;
    private final ObjectMapper objectMapper;

    public SenderServiceImpl(SenderRepository senderRepository,
                             SenderPropertyCrypto propertyCrypto,
                             ObjectMapper objectMapper) {
        this.senderRepository = senderRepository;
        this.propertyCrypto = propertyCrypto;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Sender> createSender(Sender sender) {
        Sender toSave = new Sender();
        toSave.setId(null);
        toSave.setName(sender.getName());
        toSave.setType(sender.getType());
        toSave.setDescription(sender.getDescription());
        long timestamp = Instant.now().toEpochMilli();
        toSave.setCreatedAt(timestamp);
        toSave.setUpdatedAt(timestamp);
        Map<String, Object> properties = normalizeProperties(sender.getProperties());
        Map<String, Object> encrypted = propertyCrypto.encryptSensitive(properties);
        toSave.setPropertiesJson(writeProperties(encrypted));
        return senderRepository.save(toSave)
                .map(this::enrichSender);
    }

    @Override
    public Mono<Sender> getSenderById(String id) {
        return senderRepository.findById(id)
                .map(this::enrichSender);
    }

    @Override
    public Flux<Sender> listSenders() {
        return senderRepository.findAll()
                .map(this::enrichSender);
    }

    @Override
    public Mono<Sender> updateSender(String id, Sender sender) {
        return senderRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(sender.getName());
                    existing.setType(sender.getType());
                    existing.setDescription(sender.getDescription());
                    existing.setUpdatedAt(Instant.now().toEpochMilli());
                    Map<String, Object> properties = normalizeProperties(sender.getProperties());
                    Map<String, Object> encrypted = propertyCrypto.encryptSensitive(properties);
                    existing.setPropertiesJson(writeProperties(encrypted));
                    return senderRepository.save(existing);
                })
                .map(this::enrichSender);
    }

    @Override
    public Mono<Void> deleteSender(String id) {
        return senderRepository.deleteById(id);
    }

    private Sender enrichSender(Sender sender) {
        Map<String, Object> properties = readProperties(sender.getPropertiesJson());
        Map<String, Object> decrypted = propertyCrypto.decryptSensitive(properties);
        sender.setProperties(decrypted);
        return sender;
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
            System.err.println("Failed to read sender properties: " + ex.getMessage());
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
            System.err.println("Failed to write sender properties: " + ex.getMessage());
            return "{}";
        }
    }
}
