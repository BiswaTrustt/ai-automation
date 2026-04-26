package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_expected_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiExpectedResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "validation_key", nullable = false, length = 255)
    private String validationKey;

    @Column(name = "expected_value", columnDefinition = "TEXT")
    private String expectedValue;
}
