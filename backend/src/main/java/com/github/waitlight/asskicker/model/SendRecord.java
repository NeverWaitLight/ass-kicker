package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_send_record")
public class SendRecord {

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

    @Field("recipients")
    private List<String> recipients;

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
}
