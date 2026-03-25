package com.github.waitlight.asskicker.dto.channelprovider;

import java.util.List;

public record ChannelProviderPageResponse(
        List<ChannelProviderDTO> items,
        int page,
        int size,
        long total) {
}
