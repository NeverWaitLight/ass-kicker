package com.github.waitlight.asskicker.channel;

import java.util.Map;

/**
 * Unified outbound message request for all channel types (SMS, IM, email,
 * push).
 *
 * @param recipient  Target address: phone (SMS), email (SMTP/HTTP email),
 *                   device or FCM token (push).
 *                   For IM channels the webhook configuration usually defines
 *                   the audience; this may be empty.
 * @param subject    Subject or title. Required for email; notification title
 *                   for push; for IM, if non-blank,
 *                   shown as a title line above the body. Unused for SMS.
 * @param content    Message body: template variables or full text (SMS), body
 *                   (IM/email), notification body (push).
 * @param attributes Optional key-value metadata. Merged into the JSON body by
 *                   the HTTP email channel; other
 *                   implementations may read it when needed. May be
 *                   {@code null}.
 */
public record MsgReq(
        String recipient,
        String subject,
        String content,
        Map<String, Object> attributes) {
}
