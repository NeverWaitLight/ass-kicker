package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.TemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MessageTemplateConverter {

    MessageTemplateDTO toDto(TemplateEntity entity);

    TemplateEntity toEntity(MessageTemplateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TemplateEntity copyForCreate(TemplateEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(TemplateEntity patch, @MappingTarget TemplateEntity target);
}
