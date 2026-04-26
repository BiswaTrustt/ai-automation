package com.example.apiframework.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity mapping to the {@code api_request_templates} table.
 *
 * <p>Stores the JSON body template for a given API. The {@code requestTemplate}
 * field is stored as PostgreSQL {@code JSONB} and exposed as a {@link String}
 * so that placeholder resolution can be performed via simple string operations.</p>
 */
@Entity
@Table(name = "api_request_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "apiMaster")
public class ApiRequestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiMaster apiMaster;

    /**
     * The JSON body template as a string.
     * Stored as JSONB in PostgreSQL for indexing and query support.
     * May contain {@code ${PLACEHOLDER}} tokens.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_template", nullable = false, columnDefinition = "jsonb")
    private String requestTemplate;
}
