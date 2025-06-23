package com.advanced.personservice.repository;

import com.advanced.personservice.model.Individual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IndividualRepository extends JpaRepository<Individual, UUID>, RevisionRepository<Individual, UUID, Integer> {

    Individual findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

}
