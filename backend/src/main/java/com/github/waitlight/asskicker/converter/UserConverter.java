package com.github.waitlight.asskicker.converter;

import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.model.UserEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserView toView(UserEntity user);

    @Mapping(target = "passwordHash", ignore = true)
    UserEntity toEntity(UserView view);

    @AfterMapping
    default void clearPasswordHash(UserView view, @MappingTarget UserEntity user) {
        if (user != null) {
            user.setPasswordHash(null);
        }
    }
}
