package com.github.waitlight.asskicker.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

@Mapper(componentModel = "spring", uses = ChannelProviderPropertiesMapper.class)
public interface ChannelProviderConverter {

    @Mapping(target = "properties", qualifiedByName = "channelProviderPropertiesToJson")
    ChannelProviderDTO toDto(ChannelProviderEntity entity);

    @Mapping(target = "properties", qualifiedByName = "channelProviderJsonToProperties")
    ChannelProviderEntity toEntity(ChannelProviderDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "properties", qualifiedByName = "channelProviderJsonToProperties")
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
