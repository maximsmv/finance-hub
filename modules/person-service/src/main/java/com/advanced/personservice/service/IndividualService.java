package com.advanced.personservice.service;

import com.advanced.contract.model.IndividualDto;
import com.advanced.personservice.mapper.IndividualMapper;
import com.advanced.personservice.model.Individual;
import com.advanced.personservice.model.User;
import com.advanced.personservice.repository.IndividualRepository;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndividualService {

    private final IndividualRepository individualRepository;
    private final IndividualMapper individualMapper;

    public Individual createIndividual(@Nullable IndividualDto individualDto, User user) {
        if (Objects.isNull(individualDto)) {
            return null;
        }

        Individual individual = toEntity(individualDto);
        individual.setUser(user);
        return individualRepository.save(individual);
    }

    @Transactional
    public Individual updateIndividual(@Nullable IndividualDto individualDto, User user) {
        if (Objects.isNull(individualDto)) {
            return null;
        }

        if (Objects.isNull(individualDto.getId())) {
            return createIndividual(individualDto, user);
        }

        Individual individual = individualRepository.findById(individualDto.getId())
                .orElseThrow(EntityNotFoundException::new);
        individualMapper.updateIndividualFromDto(individualDto, individual);
        individual.setUser(user);

        return individualRepository.save(individual);
    }

    public void deleteByUserId(UUID userId) {
        Individual individual = individualRepository.findByUserId(userId);
        individual.setUser(null);
        individual.setArchivedAt(LocalDateTime.now());
        individualRepository.save(individual);
    }

    public IndividualDto toDto(Individual individual) {
        return individualMapper.toDto(individual);
    }

    public Individual toEntity(IndividualDto individualDto) {
        return individualMapper.toEntity(individualDto);
    }

    public Individual getByUserId(UUID userId) {
        return individualRepository.findByUserId(userId);
    }
}
