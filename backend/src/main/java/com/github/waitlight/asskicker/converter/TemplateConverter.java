package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.template.TemplateDTO;
import com.github.waitlight.asskicker.model.TemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TemplateConverter {

    TemplateDTO toDto(TemplateEntity entity);

    TemplateEntity toEntity(TemplateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TemplateEntity copyForCreate(TemplateEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(TemplateEntity patch, @MappingTarget TemplateEntity target);
}