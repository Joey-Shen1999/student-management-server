package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.GoalTaskCompletionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoalTaskCompletionRepository extends JpaRepository<GoalTaskCompletionEntry, Long> {
    List<GoalTaskCompletionEntry> findByGoalTask_IdIn(Collection<Long> goalTaskIds);

    Optional<GoalTaskCompletionEntry> findByGoalTask_IdAndOccurrenceKey(Long goalTaskId, String occurrenceKey);
}
