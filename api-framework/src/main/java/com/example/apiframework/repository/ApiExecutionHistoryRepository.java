package com.example.apiframework.repository;

import com.example.apiframework.entity.ApiExecutionHistory;
import com.example.apiframework.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link ApiExecutionHistory}.
 *
 * <p>Provides paged and filtered access to execution audit records.</p>
 */
@Repository
public interface ApiExecutionHistoryRepository extends JpaRepository<ApiExecutionHistory, Long> {

    /**
     * Returns a page of execution history records for a given API, ordered
     * most-recent first.
     *
     * @param apiId    the parent API identifier
     * @param pageable paging/sorting spec
     * @return a page of matching history records
     */
    Page<ApiExecutionHistory> findByApiMasterApiIdOrderByCreatedAtDesc(Long apiId, Pageable pageable);

    /**
     * Returns all executions with a given status in a time window.
     *
     * @param status    the execution outcome filter
     * @param startTime window start (inclusive)
     * @param endTime   window end (inclusive)
     * @return matching records ordered by creation time descending
     */
    @Query("""
            SELECT h FROM ApiExecutionHistory h
            WHERE h.executionStatus = :status
              AND h.createdAt BETWEEN :startTime AND :endTime
            ORDER BY h.createdAt DESC
            """)
    List<ApiExecutionHistory> findByStatusAndTimeRange(
            @Param("status") ExecutionStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Counts executions grouped by status for a specific API.
     *
     * @param apiId the parent API identifier
     * @return list of {@code [ExecutionStatus, count]} projections
     */
    @Query("""
            SELECT h.executionStatus AS status, COUNT(h) AS total
            FROM ApiExecutionHistory h
            WHERE h.apiMaster.apiId = :apiId
            GROUP BY h.executionStatus
            """)
    List<Object[]> countByStatusForApi(@Param("apiId") Long apiId);
}
