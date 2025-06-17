package com.advanced.personservice.controller;

import com.advanced.personservice.dto.CountryDto;
import com.advanced.personservice.service.CountryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/countries")
public class CountryRestControllerV1 {

    private final CountryService countryService;

    @GetMapping
    public List<CountryDto> getUserById() {
        return countryService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCountry(@NotNull @Valid @RequestBody CountryDto countryDto) {
        countryService.create(countryDto);
    }

}
