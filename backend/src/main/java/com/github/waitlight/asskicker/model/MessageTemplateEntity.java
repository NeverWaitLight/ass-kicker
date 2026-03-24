package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "message_template")
@CompoundIndex(name = "uk_message_template_code_channelType", def = "{'code': 1, 'channelType': 1}", unique = true)
public class MessageTemplateEntity {

        @Id
        private String id;

        private String code;

        private ChannelType channelType;

        private JsonNode templates = JsonNodeFactory.instance.objectNode();

        private JsonNode channels = JsonNodeFactory.instance.objectNode();

        private Long createdAt;

        private Long updatedAt;
}
