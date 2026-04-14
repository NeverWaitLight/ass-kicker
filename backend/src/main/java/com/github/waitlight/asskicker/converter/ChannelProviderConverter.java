package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.model.ChannelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;

@Mapper(componentModel = "spring", uses = ChannelProviderPropertiesMapper.class)
public interface ChannelProviderConverter {

    @Mapping(target = "properties", qualifiedByName = "channelProviderPropertiesToJson")
    ChannelProviderDTO toDto(ChannelEntity entity);

    @Mapping(target = "properties", qualifiedByName = "channelProviderJsonToProperties")
    ChannelEntity toEntity(ChannelProviderDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "properties", qualifiedByName = "channelProviderJsonToProperties")
    void merge(ChannelProviderDTO dto, @MappingTarget ChannelEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChannelEntity copyForCreate(ChannelEntity source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(ChannelEntity patch, @MappingTarget ChannelEntity entity);
}
