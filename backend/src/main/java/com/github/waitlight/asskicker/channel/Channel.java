package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.config.ChannelDebugProperties;

import java.util.UUID;

public abstract class Channel<C extends ChannelSpec> {

    protected final C spec;
    protected final ChannelDebugProperties debugProperties;

    public Channel(C spec, ChannelDebugProperties debugProperties) {
        this.spec = spec;
        this.debugProperties = debugProperties;
    }

    public final MsgResp send(MsgReq request) {
        if (debugProperties.isEnabled()) {
            int sleepMs = debugProperties.getSleepMs();
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return MsgResp.failure("SEND_INTERRUPTED", ex.getMessage());
            }
            return MsgResp.success("DEBUG-" + UUID.randomUUID());
        }
        return doSend(request);
    }

    protected abstract MsgResp doSend(MsgReq request);
}
