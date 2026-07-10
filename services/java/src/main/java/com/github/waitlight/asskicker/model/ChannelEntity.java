package com.github.waitlight.asskicker.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "channels")
public class ChannelEntity extends Auditable {

        @Indexed(name = "uk_channels_code", unique = true)
        private String code;

        private String name;

        @Field("type")
        private ChannelType type;

        @Field("provider")
        private ChannelProvider provider;

        private String description;

        private boolean enabled = true;

        /**
         * 供应商配置，用于存储供应商的配置
         */
        private Map<String, String> properties = new HashMap<>();
}
