package com.advanced.personservice.repository;

import com.advanced.personservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, RevisionRepository<User, UUID, Integer> {
}
