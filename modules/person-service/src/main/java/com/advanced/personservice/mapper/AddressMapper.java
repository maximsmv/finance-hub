package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.AddressDto;
import com.advanced.personservice.model.Address;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {

    AddressDto toDto(Address address);

    @Mapping(target = "country", ignore = true)
    Address toEntity(AddressDto addressDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "archived", ignore = true)
    void updateAddressFromDto(AddressDto addressDto, @MappingTarget Address address);

}
