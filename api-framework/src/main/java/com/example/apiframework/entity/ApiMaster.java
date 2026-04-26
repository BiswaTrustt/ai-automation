package com.example.apiframework.entity;

import com.example.apiframework.enums.AuthType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity mapping to the {@code api_master} table.
 *
 * <p>Holds the core definition of a registered API: its URL, HTTP method,
 * authentication strategy, and content-type. Child entities (headers, templates,
 * validations) are lazily loaded to keep reads efficient.</p>
 */
@Entity
@Table(name = "api_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"headers", "requestTemplates", "validations"})
public class ApiMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_id")
    private Long apiId;

    @Column(name = "api_name", nullable = false, unique = true, length = 255)
    private String apiName;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    @Builder.Default
    private AuthType authType = AuthType.NONE;

    @Column(name = "content_type", nullable = false, length = 100)
    @Builder.Default
    private String contentType = "application/json";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "apiMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    @Builder.Default
    private List<ApiHeader> headers = new ArrayList<>();

    @OneToMany(mappedBy = "apiMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ApiRequestTemplate> requestTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "apiMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ApiValidation> validations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
