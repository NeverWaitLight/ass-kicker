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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChannelManagerRefreshFailureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ChannelProviderRepository channelProviderRepository;

    @Mock
    private ChannelFactory channelFactory;

    private ChannelManager channelManager;

    @BeforeEach
    void setUp() {
        channelManager = new ChannelManager(channelProviderRepository, channelFactory);
    }

    @Test
    void refresh_failure_keepsExistingCache() throws Exception {
        String json = """
                {
                  "id": "id-1",
                  "code": "apns-refresh",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "enabled": true,
                  "properties": {
                    "url": "https://api.sandbox.push.apple.com",
                    "bundleIdTopic": "com.example.app",
                    "teamId": "TEST_TEAM_ID",
                    "keyId": "TEST_KEY_ID",
                    "privateKeyPem": "-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBNZQdW2XALI6odi\\nsffzbONZ5+i8V1xxzKs88K2KPhShRANCAARfjzU68VEgfLL0eZ38qjls03GvRFwJ\\nNQLOBe4rsFB6lqOYiNME6oCVt4o5Ju46ca2RWappiw8v21uMLZPTLjx5\\n-----END PRIVATE KEY-----\\n"
                  }
                }
                """;
        ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
        ApnsChannelHandler handler = new ApnsChannelHandler(entity);
        ChannelManager.ChannelHandlerWrapper wrapper = new ChannelManager.ChannelHandlerWrapper(entity, handler);
        ConcurrentHashMap<String, ChannelManager.ChannelHandlerWrapper> map = new ConcurrentHashMap<>();
        map.put("id-1", wrapper);
        ReflectionTestUtils.setField(channelManager, "cache", map);

        when(channelProviderRepository.findAll()).thenReturn(Flux.error(new RuntimeException("db unavailable")));

        channelManager.refresh();

        assertThat(channelManager.getHandlerCount()).isEqualTo(1);
    }
}
