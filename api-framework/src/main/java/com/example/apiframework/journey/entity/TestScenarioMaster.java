package com.example.apiframework.journey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_scenario_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestScenarioMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_code", nullable = false, unique = true, length = 100)
    private String scenarioCode;

    @Column(name = "scenario_name", nullable = false, length = 255)
    private String scenarioName;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    /** TRUE for JMX-style flows that walk across many modules within one scenario. */
    @Column(name = "cross_module", nullable = false)
    @Builder.Default
    private Boolean crossModule = Boolean.FALSE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
