package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journey_execution_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneyExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journey_run_id", nullable = false, length = 64)
    private String journeyRunId;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(name = "scenario_code", nullable = false, length = 100)
    private String scenarioCode;

    @Column(name = "loan_product_code", length = 50)
    private String loanProductCode;

    @Column(name = "api_name", nullable = false, length = 255)
    private String apiName;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "db_validation_result", columnDefinition = "TEXT")
    private String dbValidationResult;

    @Column(name = "csv_validation_result", columnDefinition = "TEXT")
    private String csvValidationResult;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
