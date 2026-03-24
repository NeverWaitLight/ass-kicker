package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.messagetemplate.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MessageTemplateConverter {

    MessageTemplateDTO toDto(MessageTemplateEntity entity);

    MessageTemplateEntity toEntity(MessageTemplateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MessageTemplateEntity copyForCreate(MessageTemplateEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(MessageTemplateEntity patch, @MappingTarget MessageTemplateEntity target);
}
