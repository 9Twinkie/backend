package com.monitoring.core.application.ports.out.repositories;

import com.monitoring.core.domain.Engineer;

import java.util.List;
import java.util.Optional;

public interface EngineerRepository {

    Optional<Engineer> findById(Long id);

    Optional<Engineer> findByUsername(String username);

    Engineer save(Engineer engineer);

    List<Engineer> findAll();

    long countByRole(String role);

    boolean deleteById(Long id);
}
