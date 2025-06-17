package com.advanced.personservice.repository;

import com.advanced.personservice.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer>, RevisionRepository<Country, Integer, Integer> {
}
