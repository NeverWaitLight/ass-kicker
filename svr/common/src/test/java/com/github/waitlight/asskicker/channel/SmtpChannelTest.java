package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.model.ChannelType;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

class SmtpChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

    private SmtpChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        greenMail.setUser("sender@localhost", "secret");
        int port = greenMail.getSmtp().getPort();

        String providerJson = String.format("""
                {
                  "code": "smtp-test",
                  "channelType": "EMAIL",
                  "providerType": "SMTP",
                  "enabled": true,
                  "properties": {
                    "host": "localhost",
                    "port": "%d",
                    "username": "sender@localhost",
                    "password": "secret",
                    "from": "sender@localhost",
                    "sslEnabled": "false",
                    "starttls": "false"
                  }
                }
                """, port);
        ChannelEntity provider = MAPPER.readValue(providerJson, ChannelEntity.class);
        channel = new SmtpChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
    }

    @AfterEach
    void tearDown() {
        greenMail.reset();
    }

    @Test
    void send_deliversMessage() throws Exception {
        UniMessage message = new UniMessage();
        message.setTitle("subj");
        message.setContent("body text");
        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .providerType(ProviderType.SMTP)
                .recipients(java.util.Set.of("recv@localhost"))
                .build();

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("SMTP ok 1 recipient(s)")
                .verifyComplete();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("subj");
    }
}
