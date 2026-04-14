package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.channel.ChannelDTO;
import com.github.waitlight.asskicker.model.ChannelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = ChannelPropertiesMapper.class)
public interface ChannelConverter {

    @Mapping(target = "properties", qualifiedByName = "channelPropertiesToJson")
    ChannelDTO toDto(ChannelEntity entity);

    @Mapping(target = "properties", qualifiedByName = "channelJsonToProperties")
    ChannelEntity toEntity(ChannelDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "properties", qualifiedByName = "channelJsonToProperties")
    void merge(ChannelDTO dto, @MappingTarget ChannelEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChannelEntity copyForCreate(ChannelEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(ChannelEntity patch, @MappingTarget ChannelEntity entity);
}
