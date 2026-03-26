package com.github.waitlight.asskicker.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "notification_channels")
public class ChannelProviderEntity {

        @Id
        private String id;

        @Indexed(name = "uk_notification_channels_code", unique = true)
        private String code;

        private String name;

        private ChannelType channelType;
        private ChannelProviderType providerType;

        private String description;

        /**
         * 优先地址正则表达式，用于匹配优先地址
         */
        private String priorityAddressRegex;

        /**
         * 排除地址正则表达式，用于排除地址
         */
        private String excludeAddressRegex;

        private boolean enabled = true;

        /**
         * 供应商配置，用于存储供应商的配置
         */
        private Map<String, String> properties = new HashMap<>();

        private Long createdAt;
        private Long updatedAt;
}
