package com.advanced.personservice.mapper;

import com.advanced.personservice.dto.CountryDto;
import com.advanced.personservice.model.Country;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CountryMapper {

    CountryDto toDto(Country country);

    List<CountryDto> toDTOList(List<Country> countries);

    Country toEntity(CountryDto countryDto);

}
