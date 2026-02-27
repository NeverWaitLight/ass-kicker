package com.github.waitlight.asskicker.dto.sendrecord;

import java.util.List;

public record SendRecordPageResponse(List<SendRecordView> items, int page, int size, long total) {
}
