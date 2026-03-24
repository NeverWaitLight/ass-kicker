package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "notification_channels")
public class ChannelProviderEntity {

        @Id
        private String id;

        @Indexed(name = "uk_notification_channels_key", unique = true)
        private String key;

        private String name;

        private ChannelType type;

        private String provider;

        private String description;

        private String priorityAddressRegex;

        private String excludeAddressRegex;

        private boolean enabled = true;

        private JsonNode properties = JsonNodeFactory.instance.objectNode();

        private Long createdAt;

        private Long updatedAt;
}
