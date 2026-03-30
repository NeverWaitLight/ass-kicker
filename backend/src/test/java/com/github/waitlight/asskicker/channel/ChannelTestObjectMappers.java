package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

final class ChannelTestObjectMappers {

    private ChannelTestObjectMappers() {
    }

    static ObjectMapper channelObjectMapper() {
        return new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
