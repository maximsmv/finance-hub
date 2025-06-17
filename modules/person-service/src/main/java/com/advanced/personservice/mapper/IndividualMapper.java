package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.IndividualDto;
import com.advanced.personservice.model.Individual;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IndividualMapper {

    IndividualDto toDto(Individual individual);

    @Mapping(target = "user", ignore = true)
    Individual toEntity(IndividualDto individualDto);

}
