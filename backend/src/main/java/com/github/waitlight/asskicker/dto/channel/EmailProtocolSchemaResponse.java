package com.github.waitlight.asskicker.dto.channel;

import java.util.List;

public record EmailProtocolSchemaResponse(String defaultProtocol, List<EmailProtocolSchema> protocols) {

    public record EmailProtocolSchema(String protocol,
                                      String label,
                                      String propertyKey,
                                      List<EmailProtocolField> fields) {
    }

    public record EmailProtocolField(String key,
                                     String label,
                                     String type,
                                     boolean required,
                                     String defaultValue,
                                     String placeholder) {
    }
}
