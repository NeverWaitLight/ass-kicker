package com.github.waitlight.asskicker.sender;

public abstract class Sender<C extends SenderConfig> {

    protected final C config;

    public Sender(C config) {
        this.config = config;
    }

    public abstract MessageResponse send(MessageRequest request);
}