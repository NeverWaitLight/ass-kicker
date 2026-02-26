package com.github.waitlight.asskicker.sendercrypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Component
public class SenderPropertyCrypto {

    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKeySpec secretKeySpec;
    private final List<String> sensitiveKeyMatchers;
    private final SecureRandom secureRandom = new SecureRandom();

    public SenderPropertyCrypto(SenderCryptoProperties properties) {
        this.secretKeySpec = new SecretKeySpec(hashSecret(properties.getSecret()), "AES");
        this.sensitiveKeyMatchers = properties.getSensitiveKeys().stream()
                .map(SenderPropertyCrypto::normalizeKey)
                .toList();
    }

    public Map<String, Object> encryptSensitive(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }
        return transformMap(properties, true);
    }

    public Map<String, Object> decryptSensitive(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }
        return transformMap(properties, false);
    }

    private Map<String, Object> transformMap(Map<String, Object> properties, boolean encrypt) {
        Map<String, Object> transformed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                transformed.put(key, transformMap(castMap(mapValue), encrypt));
            } else if (value instanceof List<?> listValue) {
                transformed.put(key, transformList(listValue, encrypt));
            } else if (value instanceof String stringValue && isSensitiveKey(key)) {
                transformed.put(key, encrypt ? encryptStringIfNeeded(stringValue) : decryptStringIfNeeded(stringValue));
            } else {
                transformed.put(key, value);
            }
        }
        return transformed;
    }

    private List<Object> transformList(List<?> list, boolean encrypt) {
        List<Object> transformed = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> mapValue) {
                transformed.add(transformMap(castMap(mapValue), encrypt));
            } else if (item instanceof List<?> listValue) {
                transformed.add(transformList(listValue, encrypt));
            } else {
                transformed.add(item);
            }
        }
        return transformed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> value) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = normalizeKey(key);
        for (String matcher : sensitiveKeyMatchers) {
            if (!matcher.isBlank() && normalized.contains(matcher)) {
                return true;
            }
        }
        return false;
    }

    private String encryptStringIfNeeded(String value) {
        if (value == null || value.isBlank() || value.startsWith(ENCRYPTED_PREFIX)) {
            return value;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String encodedIv = Base64.getEncoder().encodeToString(iv);
            String encodedCipher = Base64.getEncoder().encodeToString(cipherText);
            return ENCRYPTED_PREFIX + encodedIv + ":" + encodedCipher;
        } catch (Exception ex) {
            System.err.println("Sender property encryption failed: " + ex.getMessage());
            return value;
        }
    }

    private String decryptStringIfNeeded(String value) {
        if (value == null || !value.startsWith(ENCRYPTED_PREFIX)) {
            return value;
        }
        String payload = value.substring(ENCRYPTED_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            return value;
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Sender property decryption failed: " + ex.getMessage());
            return value;
        }
    }

    private static byte[] hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize sender encryption", ex);
        }
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
