package com.github.waitlight.asskicker.sender;

public interface Sender {
    MessageResponse send(MessageRequest request);
}