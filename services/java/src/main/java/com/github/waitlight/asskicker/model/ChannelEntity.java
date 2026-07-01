package com.github.waitlight.asskicker.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
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
public class ChannelEntity {

        @Id
        private String id;

        @Indexed(name = "uk_channels_code", unique = true)
        private String code;

        private String name;

        @Field("type")
        private ChannelType type;

        @Field("provider")
        private ChannelProvider provider;

        @Field("provider_type")
        private ProviderType providerType;

        private String description;

        private boolean enabled = true;

        /**
         * 供应商配置，用于存储供应商的配置
         */
        private Map<String, String> properties = new HashMap<>();

        private String creator;

        private String updater;

        private Long createdAt;
        private Long updatedAt;
}
