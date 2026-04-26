package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ProductModuleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductModuleMappingRepository extends JpaRepository<ProductModuleMapping, Long> {
    Optional<ProductModuleMapping> findByProductIdAndModuleCodeAndActiveTrue(Long productId, String moduleCode);
}
