package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChannelFactory {

    private static final String CHANNEL_PACKAGE = "com.github.waitlight.asskicker.channel";

    private final WebClient webClient;
    private final ObjectMapper channelObjectMapper;

    /**
     * (ChannelType, ChannelProvider) 联合键
     */
    public record ChannelKey(ChannelType type, ChannelProvider provider) {
    }

    /**
     * Channel 元信息：扫描得到的 ChannelType、ChannelProvider 及对应 Channel 具体类
     */
    public record ChannelMeta(ChannelType type, ChannelProvider provider,
            Class<? extends Channel<?>> channelClass) {
    }

    private final Map<ChannelKey, ChannelMeta> channelMetaCache = new ConcurrentHashMap<>();

    public ChannelFactory(WebClient webClient,
            @Qualifier(ChannelObjectMapperConfig.BEAN_NAME) ObjectMapper channelObjectMapper) {
        this.webClient = webClient;
        this.channelObjectMapper = channelObjectMapper;
    }

    @PostConstruct
    void scanChannelImplementations() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Channel.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(CHANNEL_PACKAGE);

        for (BeanDefinition bd : candidates) {
            String className = bd.getBeanClassName();
            try {
                Class<?> loaded = Class.forName(className);
                if (!Channel.class.isAssignableFrom(loaded) || loaded == Channel.class) {
                    continue;
                }
                if (Modifier.isAbstract(loaded.getModifiers())) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Channel<?>> channelClass = (Class<? extends Channel<?>>) loaded;

                ChannelType type = readStatic(channelClass, "TYPE", ChannelType.class);
                ChannelProvider provider = readStatic(channelClass, "PROVIDER", ChannelProvider.class);
                if (type == null || provider == null) {
                    log.warn("Skip Channel {}: missing public static final TYPE/PROVIDER",
                            channelClass.getName());
                    continue;
                }

                channelMetaCache.put(new ChannelKey(type, provider),
                        new ChannelMeta(type, provider, channelClass));

                log.info("Scanned Channel implementation: {}/{}", type, provider);
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load Channel class: {}", className, e);
            }
        }

        log.info("Channel scan completed, found {} implementation(s)", channelMetaCache.size());
    }

    private static <T> T readStatic(Class<? extends Channel<?>> channelClass, String fieldName, Class<T> expectedType) {
        try {
            Field field = channelClass.getDeclaredField(fieldName);
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) || !expectedType.isAssignableFrom(field.getType())) {
                return null;
            }
            field.setAccessible(true);
            return expectedType.cast(field.get(null));
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to read " + fieldName + " from " + channelClass.getName(), e);
        }
    }

    public Optional<ChannelMeta> getChannelMeta(ChannelType type, ChannelProvider provider) {
        return Optional.ofNullable(channelMetaCache.get(new ChannelKey(type, provider)));
    }

    public Channel<?> create(ChannelEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ChannelEntity must not be null");
        }

        ChannelType type = entity.getType();
        ChannelProvider provider = entity.getProvider();
        if (type == null || provider == null) {
            log.warn("Skip channel {}, missing type/provider (type={}, provider={})",
                    entity.getCode(), type, provider);
            return null;
        }

        ChannelMeta meta = channelMetaCache.get(new ChannelKey(type, provider));
        if (meta == null) {
            log.warn("No Channel implementation registered for {}/{} (channel code={})",
                    type, provider, entity.getCode());
            return null;
        }

        try {
            Constructor<? extends Channel<?>> ctor = meta.channelClass()
                    .getDeclaredConstructor(ChannelEntity.class, WebClient.class, ObjectMapper.class);
            ctor.setAccessible(true);
            return ctor.newInstance(entity, webClient, channelObjectMapper);
        } catch (NoSuchMethodException e) {
            log.error("Channel {} missing required constructor (ChannelEntity, WebClient, ObjectMapper)",
                    meta.channelClass().getName(), e);
            return null;
        } catch (ReflectiveOperationException e) {
            log.error("Failed to create Channel for channel {}: {}", entity.getCode(), e.getMessage(), e);
            return null;
        }
    }
}
