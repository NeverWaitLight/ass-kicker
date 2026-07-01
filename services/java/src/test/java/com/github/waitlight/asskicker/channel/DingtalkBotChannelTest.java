package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponseBody;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendHeaders;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendRequest;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendResponse;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendResponseBody;
import com.aliyun.tea.TeaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.impl.DingtalkBotChannel;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class DingtalkBotChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private com.aliyun.dingtalkoauth2_1_0.Client oauthClient;
    @Mock
    private com.aliyun.dingtalkrobot_1_0.Client robotClient;

    private DingtalkBotChannel channel;

    private static ChannelEntity createProvider() throws Exception {
        String providerJson = """
                {
                  "code": "dingtalk-bot-test",
                  "type": "IM",
                  "provider": "DINGTALK",
                  "providerType": "DINGTALK_BOT",
                  "enabled": true,
                  "properties": {
                    "appKey": "app-key-1",
                    "appSecret": "app-secret-1",
                    "robotCode": "robot-code-1"
                  }
                }
                """;
        return MAPPER.readValue(providerJson, ChannelEntity.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        ChannelEntity provider = createProvider();
        channel = DingtalkBotChannel.forTesting(provider, WebClient.create(),
                ChannelTestObjectMappers.channelObjectMapper(), oauthClient, robotClient);
    }

    @Test
    void send_success_tokenThenGroupMessage() throws Exception {
        GetAccessTokenResponse tokenResp = new GetAccessTokenResponse();
        tokenResp.setBody(new GetAccessTokenResponseBody().setAccessToken("tok-abc"));
        when(oauthClient.getAccessToken(any(GetAccessTokenRequest.class))).thenReturn(tokenResp);

        OrgGroupSendResponse sendResp = new OrgGroupSendResponse();
        sendResp.setBody(new OrgGroupSendResponseBody().setProcessQueryKey("pqk"));
        when(robotClient.orgGroupSendWithOptions(any(OrgGroupSendRequest.class),
                any(OrgGroupSendHeaders.class), any())).thenReturn(sendResp);

        UniMessage message = new UniMessage();
        message.setTitle("告警");
        message.setContent("服务异常");
        UniAddress address = UniAddress.ofImBot(ProviderType.DINGTALK_BOT, "ch-1", "cid-open-1");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("DINGTALK_BOT ok 1 chat(s)")
                .verifyComplete();

        ArgumentCaptor<GetAccessTokenRequest> tokenCaptor = ArgumentCaptor.forClass(GetAccessTokenRequest.class);
        verify(oauthClient).getAccessToken(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getAppKey()).isEqualTo("app-key-1");
        assertThat(tokenCaptor.getValue().getAppSecret()).isEqualTo("app-secret-1");

        ArgumentCaptor<OrgGroupSendRequest> reqCaptor = ArgumentCaptor.forClass(OrgGroupSendRequest.class);
        ArgumentCaptor<OrgGroupSendHeaders> headerCaptor = ArgumentCaptor.forClass(OrgGroupSendHeaders.class);
        verify(robotClient).orgGroupSendWithOptions(reqCaptor.capture(), headerCaptor.capture(), any());
        OrgGroupSendRequest sentReq = reqCaptor.getValue();
        assertThat(sentReq.getRobotCode()).isEqualTo("robot-code-1");
        assertThat(sentReq.getOpenConversationId()).isEqualTo("cid-open-1");
        assertThat(sentReq.getMsgKey()).isEqualTo("sampleText");
        assertThat(sentReq.getMsgParam()).contains("服务异常");
        assertThat(headerCaptor.getValue().getXAcsDingtalkAccessToken()).isEqualTo("tok-abc");
    }

    @Test
    void send_groupFailure_returnsMappedException() throws Exception {
        GetAccessTokenResponse tokenResp = new GetAccessTokenResponse();
        tokenResp.setBody(new GetAccessTokenResponseBody().setAccessToken("tok-abc"));
        when(oauthClient.getAccessToken(any(GetAccessTokenRequest.class))).thenReturn(tokenResp);

        TeaException te = new TeaException(new HashMap<>() {
            {
                put("code", "400");
                put("message", "bad");
            }
        });
        when(robotClient.orgGroupSendWithOptions(any(OrgGroupSendRequest.class),
                any(OrgGroupSendHeaders.class), any())).thenThrow(te);

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.DINGTALK_BOT, "k", "cid");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("DINGTALK_BOT platform failure"))
                .verify();
    }

    @Test
    void send_emptyRecipients_returnsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.DINGTALK_BOT, "k");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("DINGTALK_BOT recipients required"))
                .verify();
    }

    @Test
    void send_tokenMissing_throwsIllegalStateException() throws Exception {
        GetAccessTokenResponse tokenResp = new GetAccessTokenResponse();
        tokenResp.setBody(new GetAccessTokenResponseBody());
        when(oauthClient.getAccessToken(any(GetAccessTokenRequest.class))).thenReturn(tokenResp);

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.DINGTALK_BOT, "k", "cid");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("token response missing accessToken"))
                .verify();
    }
}
