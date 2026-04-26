package com.example.apiframework.entity;

import com.example.apiframework.enums.ValidationType;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity mapping to the {@code api_validations} table.
 *
 * <p>Defines a single assertion rule evaluated against the HTTP response.
 * Multiple validation rules can be associated with one {@link ApiMaster}.
 * When {@code mandatory} is {@code true}, a failure marks the entire
 * execution as {@code FAILURE}.</p>
 */
@Entity
@Table(name = "api_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "apiMaster")
public class ApiValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validation_id")
    private Long validationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiMaster apiMaster;

    /**
     * JsonPath expression to extract a value from the response body.
     * May be {@code null} for {@code STATUS_CODE} validations.
     */
    @Column(name = "json_path", length = 500)
    private String jsonPath;

    /**
     * The value the extracted field must equal (or match, for REGEX).
     * May be {@code null} for {@code NOT_NULL} validations.
     */
    @Column(name = "expected_value", length = 1000)
    private String expectedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_type", nullable = false, length = 50)
    private ValidationType validationType;

    /** If {@code true}, a failure marks the execution result as {@code FAILURE}. */
    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private Boolean mandatory = Boolean.TRUE;
}
