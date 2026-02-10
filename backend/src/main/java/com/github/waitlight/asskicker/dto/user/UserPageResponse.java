package com.github.waitlight.asskicker.dto.user;

import java.util.List;

public record UserPageResponse(List<UserView> items, int page, int size, long total) {
}
