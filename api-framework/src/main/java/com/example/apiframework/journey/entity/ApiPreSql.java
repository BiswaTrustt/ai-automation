package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_pre_sql")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiPreSql {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "sql_order", nullable = false)
    private Integer sqlOrder;

    @Column(name = "sql_query", nullable = false, columnDefinition = "TEXT")
    private String sqlQuery;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
