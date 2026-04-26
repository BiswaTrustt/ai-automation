package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.LoanProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanProductMasterRepository extends JpaRepository<LoanProductMaster, Long> {
    Optional<LoanProductMaster> findByProductCodeAndActiveTrue(String productCode);
}
