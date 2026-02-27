package com.github.waitlight.asskicker.channels;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 通道发送请求的统一入参，供 SMS、IM、Email、Push 各通道使用。
 */
@Data
@Builder
public class MsgReq {

    /**
     * 接收方地址。SMS 填手机号，Email 填收件人邮箱，Push 填 APNs device token 或 FCM token；IM 通道由配置的
     * webhook 决定接收方，此字段可不填。
     */
    private String recipient;

    /**
     * 标题/主题。Email 必填邮件主题；Push 填通知标题；IM 若有则与 content 拼成「【subject】\n content」；SMS
     * 不使用。
     */
    private String subject;

    /**
     * 正文内容。各通道均使用：SMS 作为模板变量或全文，IM/Email 为消息体，Push 为通知 body。
     */
    private String content;

    /**
     * 扩展属性，键值对。用于透传通道相关或业务元数据；仅 HTTP 邮件通道会将其合并到请求 body，其余通道可选择性解析。
     */
    private Map<String, Object> attributes;
}