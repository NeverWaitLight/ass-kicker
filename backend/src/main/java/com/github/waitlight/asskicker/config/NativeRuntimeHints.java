package com.github.waitlight.asskicker.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class NativeRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("application.yml");
        hints.resources().registerPattern("application-native.yml");
        hints.resources().registerPattern("logback-spring.xml");
    }
}
