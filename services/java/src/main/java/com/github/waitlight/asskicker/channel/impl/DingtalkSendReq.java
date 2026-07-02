package com.github.waitlight.asskicker.channel.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DingtalkSendReq extends SendReq {
    private List<String> openConversationIds;
    private String content;
    private String title;
}
