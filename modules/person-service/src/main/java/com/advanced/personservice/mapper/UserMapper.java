package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.UserDto;
import com.advanced.personservice.model.User;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "individual", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "address", ignore = true)
    User toEntity(UserDto userDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "address", ignore = true)
    void updateUserFromDto(UserDto userDto, @MappingTarget User user);

}
