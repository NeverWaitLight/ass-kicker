package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidatorFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Channel 配置属性验证器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelConfigurer {

    private final ChannelManager channelManager;
    private final Validator validator;
    @Qualifier(ChannelObjectMapperConfig.BEAN_NAME)
    private final ObjectMapper objectMapper;

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
        ChannelManager.ChannelMeta meta = channelManager.getChannelMeta(providerType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ProviderType: " + providerType));

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
