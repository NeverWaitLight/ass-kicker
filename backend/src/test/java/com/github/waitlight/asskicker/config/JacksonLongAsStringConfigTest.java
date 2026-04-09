package com.github.waitlight.asskicker.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.common.PageResp;

class JacksonLongAsStringConfigTest {

        private static ObjectMapper newMapperWithLongAsString() {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(JacksonLongAsStringConfig.longAsStringModule());
                return mapper;
        }

        @Test
        void serializesLongAndPrimitiveLongAsJsonStrings() throws Exception {
                ObjectMapper mapper = newMapperWithLongAsString();

                PageResp<String> page = new PageResp<>(List.of("a"), 0, 10, 99L);
                String json = mapper.writeValueAsString(page);

                assertThat(json).contains("\"total\":\"99\"");
        }

        @Test
        void deserializesLongFromJsonStringOrNumber() throws Exception {
                ObjectMapper mapper = newMapperWithLongAsString();

                PageResp<?> fromString = mapper.readValue(
                                "{\"items\":[],\"page\":0,\"size\":10,\"total\":\"42\"}",
                                PageResp.class);
                assertThat(fromString.total()).isEqualTo(42L);

                PageResp<?> fromNumber = mapper.readValue(
                                "{\"items\":[],\"page\":0,\"size\":10,\"total\":43}",
                                PageResp.class);
                assertThat(fromNumber.total()).isEqualTo(43L);
        }
}
