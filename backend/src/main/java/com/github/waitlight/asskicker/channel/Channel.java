package com.github.waitlight.asskicker.channel;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class Channel {

    private final String id;
    private final String code;
    private final ChannelType channelType;
    private final boolean enabled;
    private final Pattern priorityPattern;
    private final Pattern excludePattern;

    protected final WebClient webClient;

    protected Channel(ChannelProviderEntity entity, WebClient webClient) {
        this.webClient = webClient;
        this.id = entity.getId();
        this.code = entity.getCode();
        this.channelType = entity.getChannelType();
        this.enabled = entity.isEnabled();
        this.priorityPattern = compilePattern(entity.getCode(), entity.getPriorityAddressRegex(),
                "priorityAddressRegex");
        this.excludePattern = compilePattern(entity.getCode(), entity.getExcludeAddressRegex(),
                "excludeAddressRegex");
    }

    public final Mono<String> send(UniMessage uniMessage, UniAddress uniAddress) {
        return doSend(uniMessage, uniAddress);
    }

    protected abstract Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress);

    private static Pattern compilePattern(String channelCode, String regex, String fieldName) {
        if (StringUtils.isBlank(regex)) {
            return null;
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            log.error("Invalid {} for channel {}: {}", fieldName, channelCode, regex, e);
            return null;
        }
    }

    boolean matchesPriority(String address) {
        String a = address == null ? "" : address;
        return priorityPattern != null && priorityPattern.matcher(a).matches();
    }

    boolean matchesExclude(String address) {
        String a = address == null ? "" : address;
        return excludePattern != null && excludePattern.matcher(a).matches();
    }
}
