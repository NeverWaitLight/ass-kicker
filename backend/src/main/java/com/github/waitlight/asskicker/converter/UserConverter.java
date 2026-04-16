package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.user.CreateUserDTO;
import com.github.waitlight.asskicker.dto.user.UpdateUserDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(UserEntity user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    UserEntity toEntity(CreateUserDTO dto);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    UserEntity toEntity(UpdateUserDTO dto);

}