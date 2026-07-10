package com.github.waitlight.asskicker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "templates")
public class TemplateEntity implements Auditable {

        @Id
        private String id;

        @Indexed(name = "uk_template_code", unique = true)
        private String code;

        private String name;

        private ChannelType channelType;

        /**
         * 是否将模板托管至服务商，由服务商负责渲染与发送
         */
        private boolean providerManaged = false;

        private String creator;
        private String updater;
        private Long createdAt;
        private Long updatedAt;
}
