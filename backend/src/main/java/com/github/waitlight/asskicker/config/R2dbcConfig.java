package com.github.waitlight.asskicker.config;

import com.github.waitlight.asskicker.model.ChannelType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(StoreConversions.NONE, List.of(
                new ChannelTypeReadConverter(),
                new ChannelTypeWriteConverter()
        ));
    }

    @ReadingConverter
    static class ChannelTypeReadConverter implements Converter<String, ChannelType> {
        @Override
        public ChannelType convert(String source) {
            return ChannelType.fromString(source);
        }
    }

    @WritingConverter
    static class ChannelTypeWriteConverter implements Converter<ChannelType, String> {
        @Override
        public String convert(ChannelType source) {
            return source == null ? null : source.name();
        }
    }
}
