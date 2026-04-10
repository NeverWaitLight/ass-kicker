package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.model.UserEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toView(UserEntity user);

    @Mapping(target = "password", ignore = true)
    UserEntity toEntity(UserVO view);

    @AfterMapping
    default void clearPassword(UserVO view, @MappingTarget UserEntity user) {
        if (user != null) {
            user.setPassword(null);
        }
    }
}
