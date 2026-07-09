package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelProvider;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 触发本地模板向服务商同步的请求体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLocalizedTemplateDTO {

    @NotNull
    private ChannelProvider provider;

    /**
     * 指定使用哪个 ChannelEntity 中的凭证；留空时选择与 provider 匹配的首个启用渠道。
     */
    private String channelId;

    /**
     * Aliyun：0 验证码, 1 通知, 2 推广短信, 3 国际/港澳台。
     * Tencent：0 普通短信, 1 营销短信。
     * 留空时默认使用各同步器的默认值（1）。
     */
    private Integer smsTemplateType;

    /**
     * 仅 Tencent 需要，true 表示国际/港澳台短信，缺省视为 false。
     */
    private Boolean international;

    /**
     * 提交给服务商的备注/申请说明；留空使用父模板名称。
     */
    private String remark;
}
