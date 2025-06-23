package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.IndividualDto;
import com.advanced.personservice.model.Individual;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IndividualMapper {

    IndividualDto toDto(Individual individual);

    Individual toEntity(IndividualDto individualDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    void updateIndividualFromDto(IndividualDto individualDto, @MappingTarget Individual individual);

}
