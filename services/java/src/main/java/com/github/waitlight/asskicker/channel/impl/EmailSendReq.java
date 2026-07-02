package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailSendReq extends SendReq {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private String from;
}
