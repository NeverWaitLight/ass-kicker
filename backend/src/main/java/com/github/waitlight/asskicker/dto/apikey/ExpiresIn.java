package com.github.waitlight.asskicker.dto.apikey;

/**
 * API Key 过期时间档位
 */
public enum ExpiresIn {
    DAYS_1(1),
    DAYS_7(7),
    DAYS_30(30);

    private final int days;

    ExpiresIn(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    /**
     * 计算过期时间戳（毫秒）
     *
     * @return 过期时间戳，从当前时间开始计算
     */
    public long calculateExpiresAt() {
        return System.currentTimeMillis() + (long) days * 24 * 60 * 60 * 1000;
    }
}