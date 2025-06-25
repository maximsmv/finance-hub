package com.advanced.personservice.service;

import com.advanced.contract.model.CountryDto;
import com.advanced.personservice.mapper.CountryMapper;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.repository.CountryRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final Logger log = LoggerFactory.getLogger(CountryService.class);

    private final CountryRepository countryRepository;

    private final CountryMapper countryMapper;

    public Country getCountry(@Nullable CountryDto countryDto) {
        log.debug("Get Country : {}", countryDto);
        if (Objects.isNull(countryDto) || Objects.isNull(countryDto.getId())) {
            return null;
        }

        return countryRepository.findById(countryDto.getId()).orElse(null);
    }

    public List<CountryDto> getAll() {
        return countryMapper.toDTOList(countryRepository.findAll());
    }

    public void create(CountryDto countryDto) {
        log.debug("Create Country : {}", countryDto);
        countryRepository.save(countryMapper.toEntity(countryDto));
    }

    public CountryDto toDto(Country country) {
        return countryMapper.toDto(country);
    }
}
