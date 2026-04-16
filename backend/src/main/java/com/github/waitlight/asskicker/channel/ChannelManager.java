package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.channel.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.service.ChannelService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.hibernate.validator.HibernateValidatorFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelManager {

    private static final String CHANNEL_PACKAGE = "com.github.waitlight.asskicker.channel";
    private static final Comparator<Channel> BY_CODE = Comparator
            .comparing(Channel::getCode);

    private final ChannelService channelService;
    private final ChannelFactory channelFactory;
    private final Validator validator;
    @Qualifier(ChannelObjectMapperConfig.BEAN_NAME)
    private final ObjectMapper objectMapper;

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

    /**
     * 获取指定 ProviderType 对应的属性类
     *
     * @param providerType 服务提供商类型
     * @return 属性类，如果不存在则返回 Optional.empty()
     */
    public Optional<Class<?>> getPropertiesClass(ProviderType providerType) {
        return getChannelMeta(providerType).map(ChannelMeta::propertyClass);
    }

    /**
     * 验证指定 ProviderType 的 properties 配置
     * <p>
     * 使用 Jakarta Validation 对 properties 进行校验。将 Map&lt;String, Object&gt; 转换为
     * 对应的属性对象（如 AliyunSmsChannel.Spec），然后执行 Bean Validation。
     *
     * @param providerType 服务提供商类型
     * @param properties   属性配置 Map
     * @param <T>          属性对象类型
     * @throws IllegalArgumentException 如果 ProviderType 未注册或属性类无法实例化
     */
    public <T> void validateProperties(ProviderType providerType, Map<String, Object> properties) {
        Set<ConstraintViolation<T>> violations = doValidateProperties(providerType, properties);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Properties validation failed: " + errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Set<ConstraintViolation<T>> doValidateProperties(ProviderType providerType,
            Map<String, Object> properties) {
        ChannelMeta meta = channelMetaCache.get(providerType);
        if (meta == null) {
            throw new IllegalArgumentException("Unknown ProviderType: " + providerType);
        }

        Class<?> propertyClass = meta.propertyClass();
        if (propertyClass == null || propertyClass == Void.class) {
            throw new IllegalArgumentException("No property class defined for ProviderType: " + providerType);
        }

        try {
            // 使用 Jackson 将 Map 转换为属性对象
            T propertyObject = (T) objectMapper.convertValue(properties, propertyClass);

            // 使用 fail-fast 模式，命中首个约束错误后立即返回
            Validator failFastValidator = validator.unwrap(HibernateValidatorFactory.class)
                    .usingContext()
                    .failFast(true)
                    .getValidator();
            return failFastValidator.validate(propertyObject);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Failed to convert properties to " + propertyClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 验证指定 ProviderType 的 properties 配置并返回是否验证通过
     *
     * @param providerType 服务提供商类型
     * @param properties   属性配置 Map
     * @return 如果验证通过返回 true，否则返回 false
     */
    public boolean isValid(ProviderType providerType, Map<String, Object> properties) {
        return doValidateProperties(providerType, properties).isEmpty();
    }
}