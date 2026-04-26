package com.example.apiframework.repository;

import com.example.apiframework.entity.ApiMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ApiMaster}.
 *
 * <p>All finder methods that load child collections use a JOIN FETCH
 * to avoid N+1 queries during execution.</p>
 */
@Repository
public interface ApiMasterRepository extends JpaRepository<ApiMaster, Long> {

    /**
     * Finds an active API by its unique logical name.
     *
     * @param apiName the logical name stored in {@code api_master.api_name}
     * @return an {@link Optional} containing the entity if found and active
     */
    Optional<ApiMaster> findByApiNameAndActiveTrue(String apiName);

    List<ApiMaster> findByModuleNameAndActiveTrue(String moduleName);

    /**
     * Eagerly loads the API with all its headers in one query.
     *
     * @param apiName the logical API name
     * @return the entity with headers collection populated
     */
    @Query("""
            SELECT am FROM ApiMaster am
            LEFT JOIN FETCH am.headers
            WHERE am.apiName = :apiName AND am.active = true
            """)
    Optional<ApiMaster> findWithHeadersByApiName(@Param("apiName") String apiName);

    /**
     * Eagerly loads the API with all its request templates.
     *
     * @param apiName the logical API name
     * @return the entity with requestTemplates collection populated
     */
    @Query("""
            SELECT am FROM ApiMaster am
            LEFT JOIN FETCH am.requestTemplates
            WHERE am.apiName = :apiName AND am.active = true
            """)
    Optional<ApiMaster> findWithTemplatesByApiName(@Param("apiName") String apiName);

    /**
     * Eagerly loads the API with all its validation rules.
     *
     * @param apiName the logical API name
     * @return the entity with validations collection populated
     */
    @Query("""
            SELECT am FROM ApiMaster am
            LEFT JOIN FETCH am.validations
            WHERE am.apiName = :apiName AND am.active = true
            """)
    Optional<ApiMaster> findWithValidationsByApiName(@Param("apiName") String apiName);
}
