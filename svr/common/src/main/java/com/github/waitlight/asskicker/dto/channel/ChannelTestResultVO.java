package com.github.waitlight.asskicker.dto.channel;

/**
 * 与前端测试弹窗约定 success 与 errorMessage
 */
public record ChannelTestResultVO(boolean success, String errorMessage) {

    public static ChannelTestResultVO ok() {
        return new ChannelTestResultVO(true, null);
    }

    public static ChannelTestResultVO fail(String message) {
        return new ChannelTestResultVO(false, message);
    }
}
