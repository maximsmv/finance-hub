package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.UserDto;
import com.advanced.personservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "address", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "address", ignore = true)
    User toEntity(UserDto userDto);

    @Mapping(target = "address", ignore = true)
    void updateEntityFromDto(UserDto userDto, @MappingTarget User user);

}
