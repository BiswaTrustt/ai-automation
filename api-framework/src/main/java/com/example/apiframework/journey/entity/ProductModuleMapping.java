package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_module_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductModuleMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(name = "module_name", length = 255)
    private String moduleName;

    @Column(name = "sequence_no", nullable = false)
    @Builder.Default
    private Integer sequenceNo = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
