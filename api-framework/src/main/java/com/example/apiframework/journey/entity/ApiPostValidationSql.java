package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_post_validation_sql")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiPostValidationSql {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "validation_name", nullable = false, length = 255)
    private String validationName;

    @Column(name = "sql_query", nullable = false, columnDefinition = "TEXT")
    private String sqlQuery;

    @Column(name = "expected_column", nullable = false, length = 255)
    private String expectedColumn;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
