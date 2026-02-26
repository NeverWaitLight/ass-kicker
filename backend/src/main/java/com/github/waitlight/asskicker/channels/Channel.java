package com.github.waitlight.asskicker.channels;

public abstract class Channel<C extends ChannelConfig> {

    protected final C config;

    public Channel(C config) {
        this.config = config;
    }

    public abstract MessageResponse send(MessageRequest request);
}