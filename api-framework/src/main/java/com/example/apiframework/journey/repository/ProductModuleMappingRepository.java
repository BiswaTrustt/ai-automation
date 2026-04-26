package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ProductModuleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductModuleMappingRepository extends JpaRepository<ProductModuleMapping, Long> {

    Optional<ProductModuleMapping> findByProductIdAndModuleCodeAndActiveTrue(Long productId, String moduleCode);

    List<ProductModuleMapping> findAllByProductIdAndActiveTrueOrderBySequenceNoAsc(Long productId);

    List<ProductModuleMapping> findAllByActiveTrueOrderBySequenceNoAsc();
}
