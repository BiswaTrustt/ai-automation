package com.example.apiframework.journey.service;

import com.example.apiframework.journey.entity.LoanProductMaster;
import com.example.apiframework.journey.repository.LoanProductMasterRepository;
import com.example.apiframework.journey.repository.ProductModuleMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final LoanProductMasterRepository productRepo;
    private final ProductModuleMappingRepository moduleMappingRepo;

    public LoanProductMaster require(String productCode) {
        return productRepo.findByProductCodeAndActiveTrue(productCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active loan product not found: " + productCode));
    }

    public void requireProductModule(Long productId, String productCode, String moduleCode) {
        moduleMappingRepo.findByProductIdAndModuleCodeAndActiveTrue(productId, moduleCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Module '" + moduleCode + "' is not mapped to product '" + productCode + "'"));
    }
}
