package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.JourneyExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JourneyExecutionHistoryRepository extends JpaRepository<JourneyExecutionHistory, Long> {
    List<JourneyExecutionHistory> findByJourneyRunIdOrderByExecutionOrderAsc(String journeyRunId);
}
