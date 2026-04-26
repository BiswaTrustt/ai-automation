package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_scenario_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiScenarioMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
