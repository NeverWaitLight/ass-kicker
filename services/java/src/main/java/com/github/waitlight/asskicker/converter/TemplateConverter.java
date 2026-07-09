package com.github.waitlight.asskicker.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.github.waitlight.asskicker.dto.template.CreateTemplateDTO;
import com.github.waitlight.asskicker.dto.template.TemplateVO;
import com.github.waitlight.asskicker.dto.template.UpdateTemplateDTO;
import com.github.waitlight.asskicker.model.TemplateEntity;

/**
 * Template 转换器，仅用于 Controller 层 DTO VO 与 Entity 之间的转换
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TemplateConverter {

    TemplateVO toVO(TemplateEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TemplateEntity toEntity(CreateTemplateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TemplateEntity toEntity(UpdateTemplateDTO dto);
}
