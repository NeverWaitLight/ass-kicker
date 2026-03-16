package com.github.waitlight.asskicker.dto.channel;

import java.util.List;

public record ImTypeInfo(String type, String label, String propertyKey, List<ImTypeFieldInfo> fields) {
}
