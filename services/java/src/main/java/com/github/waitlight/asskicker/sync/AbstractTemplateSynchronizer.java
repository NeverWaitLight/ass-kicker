package com.github.waitlight.asskicker.sync;

import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractTemplateSynchronizer implements TemplateSynchronizer {

    private final ChannelType type;
    private final ChannelProvider provider;

    protected AbstractTemplateSynchronizer() {
        TemplateSync spec = AnnotationUtils.findAnnotation(getClass(), TemplateSync.class);
        if (spec == null) {
            throw new IllegalStateException(
                    getClass().getName() + " must be annotated with @TemplateSync");
        }
        this.type = spec.type();
        this.provider = spec.provider();
    }

    @Override
    public ChannelType type() {
        return type;
    }

    @Override
    public ChannelProvider provider() {
        return provider;
    }

    protected static void requireText(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(messageKey);
        }
    }
}
