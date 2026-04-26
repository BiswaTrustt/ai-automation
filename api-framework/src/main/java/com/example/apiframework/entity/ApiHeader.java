package com.example.apiframework.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity mapping to the {@code api_headers} table.
 *
 * <p>Represents a single HTTP header belonging to a parent {@link ApiMaster}.
 * When {@code isDynamic} is {@code true}, the {@code headerValue} contains
 * one or more {@code ${PLACEHOLDER}} tokens resolved at execution time.</p>
 */
@Entity
@Table(name = "api_headers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "apiMaster")
public class ApiHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "header_id")
    private Long headerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiMaster apiMaster;

    @Column(name = "header_key", nullable = false, length = 200)
    private String headerKey;

    @Column(name = "header_value", nullable = false, length = 500)
    private String headerValue;

    /**
     * Indicates whether {@code headerValue} contains dynamic placeholders
     * that must be resolved before the request is sent.
     */
    @Column(name = "is_dynamic", nullable = false)
    @Builder.Default
    private Boolean isDynamic = Boolean.FALSE;

    /** Lower values are applied first when building the request headers map. */
    @Column(name = "sequence_no", nullable = false)
    @Builder.Default
    private Integer sequenceNo = 0;
}
