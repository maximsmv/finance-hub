package com.advanced.personservice.service;

import com.advanced.personservice.dto.CountryDto;
import com.advanced.personservice.mapper.CountryMapper;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.repository.CountryRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    private final CountryMapper countryMapper;

    public Country getCountry(@Nullable CountryDto countryDto) {
        if (Objects.isNull(countryDto)) {
            return null;
        }

        return countryRepository.findById(countryDto.getId()).orElse(null);
    }

    public List<CountryDto> getAll() {
        return countryMapper.toDTOList(countryRepository.findAll());
    }

    public void create(CountryDto countryDto) {
        countryRepository.save(countryMapper.toEntity(countryDto));
    }
}
