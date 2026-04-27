package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.EnvironmentMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentMasterRepository extends JpaRepository<EnvironmentMaster, Long> {

    Optional<EnvironmentMaster> findByEnvCodeAndActiveTrue(String envCode);

    List<EnvironmentMaster> findAllByActiveTrueOrderByEnvCodeAsc();
}
