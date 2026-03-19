package com.github.waitlight.asskicker.channels;

public abstract class Channel<C extends ChannelProperty> {

    protected final C config;

    public Channel(C config) {
        this.config = config;
    }

    public abstract MsgResp send(MsgReq request);
}