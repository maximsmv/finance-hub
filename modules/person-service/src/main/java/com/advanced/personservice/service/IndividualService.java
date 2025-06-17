package com.advanced.personservice.service;

import com.advanced.personservice.dto.IndividualDto;
import com.advanced.personservice.mapper.IndividualMapper;
import com.advanced.personservice.model.Individual;
import com.advanced.personservice.model.User;
import com.advanced.personservice.repository.IndividualRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IndividualService {

    private final IndividualRepository individualRepository;
    private final IndividualMapper individualMapper;

    public Individual createIndividual(@Nullable IndividualDto individualDto, User user) {
        if (Objects.isNull(individualDto)) {
            return null;
        }

        Individual individual = individualMapper.toEntity(individualDto);
        individual.setUser(user);
        return individualRepository.save(individual);
    }

}
