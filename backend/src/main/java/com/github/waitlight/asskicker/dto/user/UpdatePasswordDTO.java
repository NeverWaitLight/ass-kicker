package com.github.waitlight.asskicker.dto.user;

public record UpdatePasswordDTO(String oldPassword, String newPassword) {
}