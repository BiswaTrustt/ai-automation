package com.example.apiframework.entity;

import com.example.apiframework.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the {@code api_execution_history} table.
 *
 * <p>Immutable audit record created after every API execution attempt.
 * Records are append-only; updates are not performed on this entity.</p>
 */
@Entity
@Table(name = "api_execution_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiMaster apiMaster;

    /** The resolved request body sent to the API (after placeholder substitution). */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /** The raw response body received from the API. */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /** HTTP status code returned by the server. */
    @Column(name = "status_code")
    private Integer statusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 20)
    private ExecutionStatus executionStatus;

    /** Total round-trip time in milliseconds. */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /** Exception message when {@code executionStatus} is {@code ERROR}. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
