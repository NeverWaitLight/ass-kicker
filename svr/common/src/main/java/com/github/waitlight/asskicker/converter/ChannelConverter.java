package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.channel.CreateChannelDTO;
import com.github.waitlight.asskicker.dto.channel.UpdateChannelDTO;
import com.github.waitlight.asskicker.dto.channel.ChannelVO;
import com.github.waitlight.asskicker.model.ChannelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Channel 转换器，仅用于 Controller 层 DTO/VO 与 Entity 之间的转换
 */
@Mapper(componentModel = "spring", uses = ChannelPropertiesMapper.class, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ChannelConverter {

    // Entity -> VO (用于响应)
    @Mapping(target = "key", source = "code")
    @Mapping(target = "type", source = "channelType")
    @Mapping(target = "provider", source = "providerType")
    @Mapping(target = "properties", qualifiedByName = "channelPropertiesToJson")
    ChannelVO toVO(ChannelEntity entity);

    // CreateChannelDTO -> Entity (用于创建)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "key")
    @Mapping(target = "channelType", source = "type")
    @Mapping(target = "providerType", source = "provider")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "properties", qualifiedByName = "channelObjectPropertiesToProperties")
    ChannelEntity toEntity(CreateChannelDTO dto);

    // UpdateChannelDTO -> Entity (用于更新，null 值会被忽略)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", source = "key")
    @Mapping(target = "channelType", source = "type")
    @Mapping(target = "providerType", source = "provider")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "properties", qualifiedByName = "channelObjectPropertiesToProperties")
    ChannelEntity toEntity(UpdateChannelDTO dto);
}