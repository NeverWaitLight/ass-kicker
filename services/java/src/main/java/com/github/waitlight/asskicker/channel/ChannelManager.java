package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.dto.channel.ChannelPropertyFieldVO;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.service.ChannelService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
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
     * Channel 元信息记录，存储 @ChannelImpl 注解中的信息
     */
    public record ChannelMeta(ProviderType providerType, Class<?> propertyClass, Class<? extends Channel> channelClass) {
    }

    /**
     * 扫描 channel 包下所有带有 @ChannelImpl 注解的类，缓存其元信息
     */
    private void scanChannelImplementations() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ChannelImpl.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(CHANNEL_PACKAGE);

        for (BeanDefinition bd : candidates) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Channel> channelClass = (Class<? extends Channel>)
                        Class.forName(bd.getBeanClassName());

                ChannelImpl annotation = channelClass.getAnnotation(ChannelImpl.class);
                if (annotation != null) {
                    ProviderType providerType = annotation.providerType();
                    Class<?> propertyClass = annotation.propertyClass();

                    ChannelMeta meta = new ChannelMeta(providerType, propertyClass, channelClass);
                    channelMetaCache.put(providerType, meta);

                    log.info("Scanned Channel implementation: {} -> propertyClass: {}",
                            providerType, propertyClass.getSimpleName());
                }
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load Channel class: {}", bd.getBeanClassName(), e);
            } catch (ClassCastException e) {
                log.warn("Scanned class does not extend Channel: {}", bd.getBeanClassName(), e);
            }
        }

        log.info("Channel scan completed, found {} implementation(s)", channelMetaCache.size());
    }

    @PostConstruct
    void init() {
        // 扫描 @ChannelImpl 注解的类
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
                .filter(c -> !c.matchesExclude(recipient))
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        List<Channel> priorityMatches = matching.stream()
                .filter(c -> c.matchesPriority(recipient))
                .sorted(BY_CODE)
                .toList();
        List<Channel> candidates = priorityMatches.isEmpty()
                ? matching.stream().sorted(BY_CODE).toList()
                : priorityMatches;
        Channel chosen = candidates.get(0);
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
            cache.clear();
            cache.putAll(next);
            log.info("Refreshed channel cache, {} channel(s)", next.size());
        } catch (Exception e) {
            log.error("Channel cache refresh failed, keeping previous cache", e);
        } finally {
            refreshLock.unlock();
        }
    }

    public Channel getChannelById(String id) {
        return cache.get(id);
    }

    public List<Channel> getAllChannels() {
        return cache.values().stream().toList();
    }

    public int getChannelCount() {
        return cache.size();
    }

    /**
     * 获取指定 ProviderType 的 Channel 元信息
     *
     * @param providerType 服务提供商类型
     * @return Channel 元信息，如果不存在则返回 Optional.empty()
     */
    public Optional<ChannelMeta> getChannelMeta(ProviderType providerType) {
        return Optional.ofNullable(channelMetaCache.get(providerType));
    }

    /**
     * 获取所有已扫描的 Channel 元信息
     *
     * @return 不可修改的 Channel 元信息 Map
     */
    public Map<ProviderType, ChannelMeta> getAllChannelMetas() {
        return Map.copyOf(channelMetaCache);
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

    public List<ChannelPropertyFieldVO> getPropertiesSchema(ProviderType providerType) {
        Class<?> propertyClass = getPropertiesClass(providerType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ProviderType: " + providerType));
        if (propertyClass == Void.class) {
            return List.of();
        }
        if (propertyClass.isRecord()) {
            return Arrays.stream(propertyClass.getRecordComponents())
                    .map(this::toFieldVO)
                    .toList();
        }
        return Arrays.stream(propertyClass.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(this::toFieldVO)
                .toList();
    }

    private ChannelPropertyFieldVO toFieldVO(RecordComponent component) {
        return ChannelPropertyFieldVO.builder()
                .key(component.getName())
                .valueType(resolveValueType(component.getGenericType()))
                .required(isRequired(component.getDeclaredAnnotationsByType(NotBlank.class).length > 0,
                        component.getDeclaredAnnotationsByType(NotEmpty.class).length > 0,
                        component.getDeclaredAnnotationsByType(NotNull.class).length > 0))
                .build();
    }

    private ChannelPropertyFieldVO toFieldVO(Field field) {
        return ChannelPropertyFieldVO.builder()
                .key(field.getName())
                .valueType(resolveValueType(field.getGenericType()))
                .required(isRequired(field.getDeclaredAnnotationsByType(NotBlank.class).length > 0,
                        field.getDeclaredAnnotationsByType(NotEmpty.class).length > 0,
                        field.getDeclaredAnnotationsByType(NotNull.class).length > 0))
                .build();
    }

    private boolean isRequired(boolean notBlank, boolean notEmpty, boolean notNull) {
        return notBlank || notEmpty || notNull;
    }

    private String resolveValueType(Type type) {
        if (type == null) {
            return "string";
        }
        String typeName = type.getTypeName();
        if (typeName.endsWith("String")) {
            return "string";
        }
        if (typeName.endsWith("boolean") || typeName.endsWith("Boolean")) {
            return "boolean";
        }
        if (typeName.endsWith("int") || typeName.endsWith("Integer")
                || typeName.endsWith("long") || typeName.endsWith("Long")
                || typeName.endsWith("double") || typeName.endsWith("Double")
                || typeName.endsWith("float") || typeName.endsWith("Float")
                || typeName.endsWith("short") || typeName.endsWith("Short")) {
            return "number";
        }
        if (typeName.contains("Map")) {
            return "object";
        }
        if (typeName.contains("List") || typeName.endsWith("[]")) {
            return "array";
        }
        return "string";
    }

    /**
     * 获取指定 ProviderType 对应的属性类
     *
     * @param providerType 服务提供商类型
     * @return 属性类，如果不存在则返回 Optional.empty()
     */
    public Optional<Class<?>> getPropertiesClass(ProviderType providerType) {
        return getChannelMeta(providerType).map(ChannelMeta::propertyClass);
    }
}