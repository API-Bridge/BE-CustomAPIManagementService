package org.example.customapisvc.repository;

import org.example.customapisvc.domain.Entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, String> {

    List<WorkflowStep> findByCustomApiIdAndDeletedFalseOrderByExecutionGroupAsc(String customApiId);

    List<WorkflowStep> findByCustomApiIdAndExecutionGroupAndDeletedFalse(String customApiId, Integer executionGroup);

    Optional<WorkflowStep> findByStepIdAndDeletedFalse(String stepId);

    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.customApiId = :customApiId AND ws.alias = :alias AND ws.deleted = false")
    Optional<WorkflowStep> findByCustomApiIdAndAliasAndDeletedFalse(@Param("customApiId") String customApiId, @Param("alias") String alias);

    void deleteByCustomApiId(String customApiId);
}