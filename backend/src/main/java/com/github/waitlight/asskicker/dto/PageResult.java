package com.github.waitlight.asskicker.dto;

import java.util.List;

public record PageResult<T>(int page, int size, long total, List<T> data) {
}
