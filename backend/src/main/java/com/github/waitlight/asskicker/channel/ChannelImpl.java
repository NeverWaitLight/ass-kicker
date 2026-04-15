package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ProviderType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChannelImpl {
    ProviderType providerType();

    Class<?> propertyClass();
}
