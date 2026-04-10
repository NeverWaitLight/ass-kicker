package com.github.waitlight.asskicker.dto.auth;

import com.github.waitlight.asskicker.dto.user.UserVO;

public record TokenVO(String accessToken, String refreshToken, UserVO user) {
}