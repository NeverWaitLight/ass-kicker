package com.github.waitlight.asskicker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @deprecated 已被 {@link com.github.waitlight.asskicker.channel.SendReq} 体系取代。
 */
@Deprecated
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniTask {

    private UniMessage message;
    private UniAddress address;
    private String taskId;
    private Long submittedAt;
}
