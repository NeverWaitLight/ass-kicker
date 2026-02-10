package com.github.waitlight.asskicker.dto.user;

public record UpdatePasswordRequest(String oldPassword, String newPassword) {
}
