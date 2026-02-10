package com.github.waitlight.asskicker.dto.auth;

import com.github.waitlight.asskicker.dto.user.UserView;

public record TokenResponse(String accessToken, String refreshToken, UserView user) {
}
