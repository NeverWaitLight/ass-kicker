package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.service.RecordService;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
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
    private final RecordService recordService;

    /**
     * (ChannelType, ChannelProvider) 联合键
     */
    public record ChannelKey(ChannelType type, ChannelProvider provider) {
    }

    /**
     * Channel 元信息：扫描得到的 ChannelType、ChannelProvider 及对应 Channel 具体类
     */
    public record ChannelMeta(ChannelType type, ChannelProvider provider,
            Class<? extends AbstractChannel<?>> channelClass) {
    }

    private final Map<ChannelKey, ChannelMeta> channelMetaCache = new ConcurrentHashMap<>();

    public ChannelFactory(WebClient webClient,
            @Qualifier(ChannelObjectMapperConfig.BEAN_NAME) ObjectMapper channelObjectMapper,
            RecordService recordService) {
        this.webClient = webClient;
        this.channelObjectMapper = channelObjectMapper;
        this.recordService = recordService;
    }

    @PostConstruct
    void scanChannelImplementations() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractChannel.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(CHANNEL_PACKAGE);

        for (BeanDefinition bd : candidates) {
            String className = bd.getBeanClassName();
            Class<?> loaded = ClassUtils.resolveClassName(className, getClass().getClassLoader());
            if (!AbstractChannel.class.isAssignableFrom(loaded) || loaded == AbstractChannel.class
                    || Modifier.isAbstract(loaded.getModifiers())) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends AbstractChannel<?>> channelClass = (Class<? extends AbstractChannel<?>>) loaded;

            Channel spec = AnnotationUtils.findAnnotation(channelClass, Channel.class);
            if (spec == null) {
                log.warn("Skip Channel {}: missing @ChannelImpl", channelClass.getName());
                continue;
            }

            ChannelKey key = new ChannelKey(spec.type(), spec.provider());
            ChannelMeta previous = channelMetaCache.putIfAbsent(key,
                    new ChannelMeta(spec.type(), spec.provider(), channelClass));
            if (previous != null) {
                log.warn("Duplicate @ChannelImpl for {}/{}: {} already registered, {} ignored",
                        spec.type(), spec.provider(),
                        previous.channelClass().getName(), channelClass.getName());
                continue;
            }

            log.info("Scanned Channel implementation: {}/{}", spec.type(), spec.provider());
        }

        log.info("Channel scan completed, found {} implementation(s)", channelMetaCache.size());
    }

    public Optional<ChannelMeta> getChannelMeta(ChannelType type, ChannelProvider provider) {
        return Optional.ofNullable(channelMetaCache.get(new ChannelKey(type, provider)));
    }

    public AbstractChannel<?> create(ChannelEntity entity) {
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
            Constructor<? extends AbstractChannel<?>> ctor = meta.channelClass()
                    .getDeclaredConstructor(ChannelEntity.class, WebClient.class, ObjectMapper.class,
                            RecordService.class);
            ctor.setAccessible(true);
            return ctor.newInstance(entity, webClient, channelObjectMapper, recordService);
        } catch (NoSuchMethodException e) {
            log.error("Channel {} missing required constructor (ChannelEntity, WebClient, ObjectMapper, RecordService)",
                    meta.channelClass().getName(), e);
            return null;
        } catch (ReflectiveOperationException e) {
            log.error("Failed to create Channel for channel {}: {}", entity.getCode(), e.getMessage(), e);
            return null;
        }
    }
}
