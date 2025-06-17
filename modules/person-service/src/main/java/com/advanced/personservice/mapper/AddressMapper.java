package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.AddressDto;
import com.advanced.personservice.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "country", ignore = true)
    AddressDto toDto(Address address);

    @Mapping(target = "country", ignore = true)
    Address toEntity(AddressDto addressDto);

}
