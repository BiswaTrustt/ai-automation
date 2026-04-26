package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** Times to execute this step (LoopController / WhileController in JMeter). */
    @Column(name = "loop_count", nullable = false)
    @Builder.Default
    private Integer loopCount = 1;

    /** Sleep before this step (JMeter Constant Timer). */
    @Column(name = "delay_ms", nullable = false)
    @Builder.Default
    private Integer delayMs = 0;

    /** JSON object: {"varName": "$.json.path"} — captured into JourneyContext after each call. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extraction_mappings", columnDefinition = "jsonb")
    private String extractionMappings;
}
