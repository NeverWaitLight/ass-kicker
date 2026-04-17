package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.apikey.ApiKeyVO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyDTO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyVO;
import com.github.waitlight.asskicker.model.ApiKeyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiKeyConverter {

    ApiKeyVO toVO(ApiKeyEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "keyHash", ignore = true)
    @Mapping(target = "keyPrefix", ignore = true)
    @Mapping(target = "maskedRawKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ApiKeyEntity toEntity(CreateApiKeyDTO dto);

    default CreateApiKeyVO toCreateVO(ApiKeyEntity entity, String rawKey) {
        if (entity == null) {
            return null;
        }
        return new CreateApiKeyVO(
                entity.getId(),
                entity.getName(),
                rawKey,
                entity.getCreatedAt());
    }
}
