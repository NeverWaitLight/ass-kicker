package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelManager {

    private static final String CHANNEL_PACKAGE = "com.github.waitlight.asskicker.channel";
    private static final Comparator<Channel> BY_CODE = Comparator
            .comparing(Channel::getCode);

    private final ChannelService channelService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, Channel> cache = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * 缓存 Channel 元信息：(ChannelType, ChannelProvider) -> ChannelMeta
     */
    private final Map<ChannelKey, ChannelMeta> channelMetaCache = new ConcurrentHashMap<>();

    /**
     * (ChannelType, ChannelProvider) 联合键，作为 channelMetaCache 的索引
     */
    public record ChannelKey(ChannelType type, ChannelProvider provider) {
    }

    /**
     * Channel 元信息记录，保存扫描得到的 ChannelType、ChannelProvider、Channel 具体类
     */
    public record ChannelMeta(ChannelType type, ChannelProvider provider,
            Class<? extends Channel> channelClass) {
    }

    /**
     * 扫描 channel 包下所有 Channel 具体子类，读取其静态字段 TYPE 和 PROVIDER，缓存元信息
     */
    private void scanChannelImplementations() {
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
                Class<? extends Channel> channelClass = (Class<? extends Channel>) loaded;

                ChannelType type = readStatic(channelClass, "TYPE", ChannelType.class);
                ChannelProvider provider = readStatic(channelClass, "PROVIDER", ChannelProvider.class);
                if (type == null || provider == null) {
                    log.warn("Skip Channel {}: missing public static final TYPE/PROVIDER",
                            channelClass.getName());
                    continue;
                }

                ChannelMeta meta = new ChannelMeta(type, provider, channelClass);
                channelMetaCache.put(new ChannelKey(type, provider), meta);

                log.info("Scanned Channel implementation: {}/{}", type, provider);
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load Channel class: {}", className, e);
            }
        }

        log.info("Channel scan completed, found {} implementation(s)", channelMetaCache.size());
    }

    private static <T> T readStatic(Class<? extends Channel> channelClass, String fieldName, Class<T> expectedType) {
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

    @PostConstruct
    void init() {
        scanChannelImplementations();

        // 加载已启用的 Channel
        List<ChannelEntity> enabled = channelService.findEnabled().collectList().block();
        if (enabled == null) {
            enabled = List.of();
        }
        int loaded = 0;
        for (ChannelEntity entity : enabled) {
            Channel channel = channelFactory.create(entity);
            if (channel == null) {
                log.warn("Skip channel {}, channel creation returned null", entity.getCode());
                continue;
            }
            cache.put(entity.getId(), channel);
            loaded++;
        }
        log.info("Loaded {} channel channel(s)", loaded);
    }

    public Mono<Channel> chose(ChannelType channelType, String recipient) {
        List<Channel> matching = cache.values().stream()
                .filter(c -> c.getChannelType() == channelType)
                .filter(Channel::isEnabled)
                .sorted(BY_CODE)
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        Channel chosen = matching.get(0);
        log.debug("Selected channel {} for recipient {}", chosen.getCode(), recipient);
        return Mono.just(chosen);
    }

    public void refresh() {
        refreshLock.lock();
        try {
            ConcurrentHashMap<String, Channel> next = new ConcurrentHashMap<>();
            List<ChannelEntity> enabledProvider = channelService.findEnabled().collectList().block();
            if (enabledProvider == null) {
                enabledProvider = List.of();
            }

            for (ChannelEntity provider : enabledProvider) {
                Channel channel = channelFactory.create(provider);
                if (channel != null) {
                    next.put(provider.getId(), channel);
                }
            }
            List<Channel> previous = new ArrayList<>(cache.values());
            cache.clear();
            cache.putAll(next);
            log.info("Refreshed channel cache, {} channel(s)", next.size());
            disposeAll(previous);
        } catch (Exception e) {
            log.error("Channel cache refresh failed, keeping previous cache", e);
        } finally {
            refreshLock.unlock();
        }
    }

    @PreDestroy
    void shutdown() {
        disposeAll(new ArrayList<>(cache.values()));
        cache.clear();
    }

    private void disposeAll(List<Channel> channels) {
        for (Channel c : channels) {
            try {
                c.dispose();
            } catch (Exception e) {
                log.warn("Channel {} dispose failed", c.getCode(), e);
            }
        }
    }

    public int getChannelCount() {
        return cache.size();
    }

    public Optional<ChannelMeta> getChannelMeta(ChannelType type, ChannelProvider provider) {
        return Optional.ofNullable(channelMetaCache.get(new ChannelKey(type, provider)));
    }

    public List<ChannelProviderOptionVO> getProvidersByChannelType(ChannelType channelType) {
        return channelMetaCache.keySet().stream()
                .filter(key -> key.type() == channelType)
                .map(ChannelKey::provider)
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .map(provider -> ChannelProviderOptionVO.builder()
                        .value(provider.name())
                        .label(provider.name())
                        .build())
                .toList();
    }

}
