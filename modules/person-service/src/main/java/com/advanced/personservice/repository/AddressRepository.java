package com.advanced.personservice.repository;

import com.advanced.personservice.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID>, RevisionRepository<Address, UUID, Integer> {
}
