package com.github.waitlight.asskicker.dto.common;

import java.util.List;

public record PageResp<T>(List<T> items, int page, int size, long total) {
}
