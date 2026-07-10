package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 本地 LocalizedTemplate 与某条 Channel 上远端服务商模板的绑定关系。
 * 一条 LocalizedTemplate 在每个 Channel 上至多存在一条映射，
 * 因此以 (localized_template_id, channel_id) 建复合唯一索引。
 */
@Getter
@Setter
@NoArgsConstructor
@CompoundIndex(name = "uk_ct_localized_template_id_channel_id",
        def = "{'localized_template_id': 1, 'channel_id': 1}", unique = true)
@Document(collection = "channel_templates")
public class ChannelTemplateEntity extends Auditable {

    @Field("localized_template_id")
    private String localizedTemplateId;

    @Field("channel_id")
    private String channelId;

    @Field("channel_template_code")
    private String channelTemplateCode;

    @Field("uploaded_at")
    private Long uploadedAt;

    @Field("failure_reason")
    private String failureReason;
}
