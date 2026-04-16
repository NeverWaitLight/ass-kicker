package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_send_record")
public class RecordEntity {

    @Id
    private String id;

    @Field("task_id")
    private String taskId;

    @Field("template_code")
    private String templateCode;

    @Field("language_code")
    private String languageCode;

    @Field("params")
    private Map<String, Object> params;

    @Field("channel_id")
    private String channelId;

    @Field("recipient")
    private String recipient;

    @Field("submitted_at")
    private Long submittedAt;

    @Field("rendered_content")
    private String renderedContent;

    @Field("channel_type")
    private ChannelType channelType;

    @Field("channel_name")
    private String channelName;

    @Field("status")
    private SendRecordStatus status;

    @Field("error_code")
    private String errorCode;

    @Field("error_message")
    private String errorMessage;

    @Field("sent_at")
    private Long sentAt;

    /**
     * Absolute time when this document may be removed by MongoDB TTL.
     * {@code expireAfterSeconds = 0} means delete once this instant is in the past.
     */
    @Indexed(name = "idx_t_send_record_expire_at", expireAfterSeconds = 0)
    @Field("expire_at")
    private Instant expireAt;
}
