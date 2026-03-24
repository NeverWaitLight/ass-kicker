package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.channelprovider.ChannelProviderDTO;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ChannelProviderConverter {

    ChannelProviderDTO toDto(ChannelProviderEntity entity);

    ChannelProviderEntity toEntity(ChannelProviderDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(ChannelProviderDTO dto, @MappingTarget ChannelProviderEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChannelProviderEntity copyForCreate(ChannelProviderEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(ChannelProviderEntity patch, @MappingTarget ChannelProviderEntity entity);
}
