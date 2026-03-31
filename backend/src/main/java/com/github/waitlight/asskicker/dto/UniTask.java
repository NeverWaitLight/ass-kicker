package com.github.waitlight.asskicker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
