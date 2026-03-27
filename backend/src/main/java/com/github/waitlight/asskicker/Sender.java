package com.github.waitlight.asskicker;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniSendMessageReq;
import com.github.waitlight.asskicker.model.SendRecordEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final MessageTemplateEngine messageTemplateEngine;
    private final ChannelManager channelManager;
    

    public Mono<String> send(UniSendMessageReq req, UniAddress uniAddress) {
        return messageTemplateEngine.fill(req).flatMap(uniMessage -> {
            return channelManager.selectHandler(uniAddress.getChannelType(), uniAddress.getChannelProviderKey())
                    .flatMap(channelHandler -> channelHandler.send(uniMessage, uniAddress)).map(sendResult -> {
                        SendRecordEntity sendRecordEntity = new SendRecordEntity();
                        sendRecordEntity.setTaskId(null);
                        sendRecordEntity.setTemplateCode(req.getTemplateCode());
                        sendRecordEntity.setLanguageCode(req.getLanguage().getCode());
                        sendRecordEntity.setParams(req.getTemplateParams());
                        sendRecordEntity.setChannelId(uniAddress.getChannelProviderKey());
                        String recipient = null;
                        if (uniAddress.getRecipients() != null && !uniAddress.getRecipients().isEmpty()) {
                            recipient = uniAddress.getRecipients().iterator().next();
                        }
                        sendRecordEntity.setRecipient(recipient);
                        sendRecordEntity.setSubmittedAt(System.currentTimeMillis());
                        sendRecordEntity.setRenderedContent(uniMessage.getContent());
                        return sendResult;
                    });
        });
    }
}
