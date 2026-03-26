package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(classes = {
        AssKickerTestApplication.class,
        MongoTestConfiguration.class
}, properties = {
        "spring.main.web-application-type=none",
        "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class ChannelManagerMongoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String APNS_PROPS = """
            "properties": {
              "url": "https://api.sandbox.push.apple.com",
              "bundleIdTopic": "com.example.app",
              "teamId": "TEST_TEAM_ID",
              "keyId": "TEST_KEY_ID",
              "privateKeyPem": "-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBNZQdW2XALI6odi\\nsffzbONZ5+i8V1xxzKs88K2KPhShRANCAARfjzU68VEgfLL0eZ38qjls03GvRFwJ\\nNQLOBe4rsFB6lqOYiNME6oCVt4o5Ju46ca2RWappiw8v21uMLZPTLjx5\\n-----END PRIVATE KEY-----\\n"
            }
            """;

    @Autowired
    private ChannelProviderRepository channelProviderRepository;

    @Autowired
    private ChannelManager channelManager;

    @BeforeEach
    void clearProviders() {
        StepVerifier.create(channelProviderRepository.deleteAll()).verifyComplete();
        channelManager.refresh();
    }

    @Test
    void init_and_refresh_loadHandlers() throws Exception {
        assertThat(channelManager.getHandlerCount()).isZero();

        String json = String.format("""
                {
                  "code": "apns-it",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "enabled": true,
                  %s
                }
                """, APNS_PROPS);
        ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
        StepVerifier.create(channelProviderRepository.save(entity))
                .assertNext(saved -> assertThat(saved.getId()).isNotBlank())
                .verifyComplete();

        channelManager.refresh();
        assertThat(channelManager.getHandlerCount()).isEqualTo(1);
        assertThat(channelManager.getAllHandlers()).hasSize(1);
        StepVerifier.create(channelProviderRepository.findAll().collectList())
                .assertNext(list -> {
                    String id = list.get(0).getId();
                    assertThat(channelManager.getHandlerById(id)).isInstanceOf(ApnsChannelHandler.class);
                })
                .verifyComplete();
    }

    @Test
    void selectHandler_respectsPriorityAndExclude() throws Exception {
        String common = """
                "channelType": "PUSH",
                "providerType": "APNS",
                "enabled": true,
                """;

        String lowPriority = String.format("""
                {
                  "code": "z-apns",
                  %s
                  "priorityAddressRegex": "^nomatch$",
                  "excludeAddressRegex": "^$",
                  %s
                }
                """, common, APNS_PROPS);

        String highPriority = String.format("""
                {
                  "code": "a-apns",
                  %s
                  "priorityAddressRegex": "^token-",
                  "excludeAddressRegex": "^$",
                  %s
                }
                """, common, APNS_PROPS);

        String excluded = String.format("""
                {
                  "code": "ex-apns",
                  %s
                  "priorityAddressRegex": ".*",
                  "excludeAddressRegex": "^token-X",
                  %s
                }
                """, common, APNS_PROPS);

        ChannelProviderEntity e1 = MAPPER.readValue(lowPriority, ChannelProviderEntity.class);
        ChannelProviderEntity e2 = MAPPER.readValue(highPriority, ChannelProviderEntity.class);
        ChannelProviderEntity e3 = MAPPER.readValue(excluded, ChannelProviderEntity.class);

        StepVerifier.create(channelProviderRepository.saveAll(Flux.just(e1, e2, e3)).then())
                .verifyComplete();

        channelManager.refresh();

        StepVerifier.create(channelManager.selectHandler(ChannelType.PUSH, "token-abc"))
                .assertNext(h -> assertThat(h).isInstanceOf(ApnsChannelHandler.class))
                .verifyComplete();

        StepVerifier.create(channelManager.selectHandler(ChannelType.PUSH, "token-X"))
                .assertNext(h -> assertThat(h).isInstanceOf(ApnsChannelHandler.class))
                .verifyComplete();
    }

    @Test
    void selectHandler_whenAllCandidatesExcluded_returnsEmpty() throws Exception {
        String json = String.format("""
                {
                  "code": "solo-exclude",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "enabled": true,
                  "priorityAddressRegex": ".*",
                  "excludeAddressRegex": "^solo$",
                  %s
                }
                """, APNS_PROPS);
        ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
        StepVerifier.create(channelProviderRepository.save(entity))
                .assertNext(saved -> assertThat(saved.getCode()).isEqualTo("solo-exclude"))
                .verifyComplete();
        channelManager.refresh();

        StepVerifier.create(channelManager.selectHandler(ChannelType.PUSH, "solo"))
                .verifyComplete();
    }
}
