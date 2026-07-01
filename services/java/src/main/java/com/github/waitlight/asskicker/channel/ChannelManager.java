package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.service.ChannelService;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
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
     * 缓存 Channel 元信息：ProviderType -> ChannelMeta
     */
    private final Map<ProviderType, ChannelMeta> channelMetaCache = new ConcurrentHashMap<>();

    /**
     * Channel 元信息记录，保存扫描得到的 ProviderType、Properties 类、Channel 具体类
     */
    public record ChannelMeta(ProviderType providerType, Class<?> propertyClass, Class<? extends Channel> channelClass) {
    }

    /**
     * 扫描 channel 包下所有 Channel 具体子类，读取其静态字段 PROVIDER_TYPE 与内部类 Properties，
     * 缓存元信息
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

                ProviderType providerType = readProviderType(channelClass);
                if (providerType == null) {
                    log.warn("Skip Channel {}: missing public static final ProviderType PROVIDER_TYPE",
                            channelClass.getName());
                    continue;
                }
                Class<?> propertyClass = findPropertiesClass(channelClass);

                ChannelMeta meta = new ChannelMeta(providerType, propertyClass, channelClass);
                channelMetaCache.put(providerType, meta);

                log.info("Scanned Channel implementation: {} -> propertyClass: {}",
                        providerType, propertyClass.getSimpleName());
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load Channel class: {}", className, e);
            }
        }

        log.info("Channel scan completed, found {} implementation(s)", channelMetaCache.size());
    }

    private static ProviderType readProviderType(Class<? extends Channel> channelClass) {
        try {
            Field field = channelClass.getDeclaredField("PROVIDER_TYPE");
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) || !ProviderType.class.isAssignableFrom(field.getType())) {
                return null;
            }
            field.setAccessible(true);
            return (ProviderType) field.get(null);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to read PROVIDER_TYPE from " + channelClass.getName(), e);
        }
    }

    private static Class<?> findPropertiesClass(Class<? extends Channel> channelClass) {
        for (Class<?> nested : channelClass.getDeclaredClasses()) {
            if ("Properties".equals(nested.getSimpleName())) {
                return nested;
            }
        }
        return Void.class;
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

    public Optional<ChannelMeta> getChannelMeta(ProviderType providerType) {
        return Optional.ofNullable(channelMetaCache.get(providerType));
    }

    public List<ChannelProviderOptionVO> getProvidersByChannelType(ChannelType channelType) {
        return Arrays.stream(ProviderType.values())
                .filter(providerType -> providerType.getChannelType() == channelType)
                .map(providerType -> ChannelProviderOptionVO.builder()
                        .value(providerType.name())
                        .label(providerType.name())
                        .build())
                .toList();
    }

    public Schema<?> getPropertiesSchema(ProviderType providerType) {
        Class<?> propertyClass = getPropertiesClass(providerType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ProviderType: " + providerType));
        if (propertyClass == Void.class) {
            return new Schema<>().type("object");
        }
        Map<String, Schema> all = ModelConverters.getInstance().readAll(propertyClass);
        Schema<?> root = all.get(propertyClass.getSimpleName());
        return root != null ? root : new Schema<>().type("object");
    }

    public Optional<Class<?>> getPropertiesClass(ProviderType providerType) {
        return getChannelMeta(providerType).map(ChannelMeta::propertyClass);
    }
}
