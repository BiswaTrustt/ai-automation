package com.example.apiframework.service;

import com.example.apiframework.entity.ApiExecutionHistory;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.enums.ExecutionStatus;
import com.example.apiframework.repository.ApiExecutionHistoryRepository;
import com.example.apiframework.repository.ApiMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for persisting {@link ApiExecutionHistory} records.
 *
 * <p>Each {@code save} call runs in its own independent transaction
 * ({@link Propagation#REQUIRES_NEW}) so that audit records are committed
 * even when the outer execution transaction rolls back due to an error.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionHistoryService {

    private final ApiExecutionHistoryRepository historyRepository;
    private final ApiMasterRepository apiMasterRepository;

    /**
     * Persists a new execution history record.
     *
     * @param apiId           database ID of the executed API
     * @param requestPayload  the resolved request body that was sent
     * @param responsePayload the raw response body received
     * @param statusCode      HTTP response status code (-1 on network error)
     * @param status          execution outcome
     * @param executionTimeMs round-trip duration in milliseconds
     * @param errorMessage    exception message on ERROR/TIMEOUT; may be {@code null}
     * @return the persisted {@link ApiExecutionHistory} entity (with generated ID)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiExecutionHistory save(Long apiId,
                                    String requestPayload,
                                    String responsePayload,
                                    int statusCode,
                                    ExecutionStatus status,
                                    long executionTimeMs,
                                    String errorMessage) {
        ApiMaster apiMaster = apiMasterRepository.getReferenceById(apiId);

        ApiExecutionHistory history = ApiExecutionHistory.builder()
                .apiMaster(apiMaster)
                .requestPayload(requestPayload)
                .responsePayload(responsePayload)
                .statusCode(statusCode)
                .executionStatus(status)
                .executionTimeMs(executionTimeMs)
                .errorMessage(errorMessage)
                .build();

        ApiExecutionHistory saved = historyRepository.save(history);
        log.info("Execution history saved: executionId={}, apiId={}, status={}, statusCode={}, timeMs={}",
                saved.getExecutionId(), apiId, status, statusCode, executionTimeMs);
        return saved;
    }
}
