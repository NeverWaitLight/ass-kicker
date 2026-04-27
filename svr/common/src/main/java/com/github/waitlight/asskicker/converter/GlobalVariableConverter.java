package com.github.waitlight.asskicker.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.github.waitlight.asskicker.dto.globalvariable.CreateGlobalVariableDTO;
import com.github.waitlight.asskicker.dto.globalvariable.GlobalVariableVO;
import com.github.waitlight.asskicker.dto.globalvariable.UpdateGlobalVariableDTO;
import com.github.waitlight.asskicker.model.GlobalVariableEntity;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GlobalVariableConverter {

    GlobalVariableVO toVO(GlobalVariableEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GlobalVariableEntity toEntity(CreateGlobalVariableDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GlobalVariableEntity toEntity(UpdateGlobalVariableDTO dto);
}
