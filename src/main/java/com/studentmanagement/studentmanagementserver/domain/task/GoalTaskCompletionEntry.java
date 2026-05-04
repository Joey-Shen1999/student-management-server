package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "goal_task_completion_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_goal_task_completion_goal_occurrence",
                        columnNames = {"goal_task_id", "occurrence_key"}
                )
        },
        indexes = {
                @Index(name = "idx_goal_task_completion_goal", columnList = "goal_task_id"),
                @Index(name = "idx_goal_task_completion_occurrence", columnList = "occurrence_start_at"),
                @Index(name = "idx_goal_task_completion_teacher", columnList = "updated_by_teacher_id")
        }
)
public class GoalTaskCompletionEntry extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_task_id", nullable = false)
    private GoalTask goalTask;

    @Column(name = "occurrence_key", nullable = false, length = 40)
    private String occurrenceKey;

    @Column(name = "occurrence_label", nullable = false, length = 120)
    private String occurrenceLabel;

    @Column(name = "occurrence_start_at", nullable = false)
    private LocalDate occurrenceStartAt;

    @Column(name = "occurrence_end_at")
    private LocalDate occurrenceEndAt;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_teacher_id")
    private Teacher updatedByTeacher;

    @Column(name = "progress_note", nullable = false, length = 2000)
    private String progressNote;

    protected GoalTaskCompletionEntry() {
    }

    public GoalTaskCompletionEntry(GoalTask goalTask,
                                   String occurrenceKey,
                                   String occurrenceLabel,
                                   LocalDate occurrenceStartAt,
                                   LocalDate occurrenceEndAt) {
        this.goalTask = goalTask;
        this.occurrenceKey = occurrenceKey;
        this.occurrenceLabel = occurrenceLabel;
        this.occurrenceStartAt = occurrenceStartAt;
        this.occurrenceEndAt = occurrenceEndAt;
        this.completed = false;
        this.progressNote = "";
    }

    public void syncOccurrence(String occurrenceLabel, LocalDate occurrenceStartAt, LocalDate occurrenceEndAt) {
        this.occurrenceLabel = occurrenceLabel;
        this.occurrenceStartAt = occurrenceStartAt;
        this.occurrenceEndAt = occurrenceEndAt;
    }

    public void updateCompleted(boolean completed, String progressNote, Teacher updatedByTeacher) {
        this.completed = completed;
        this.completedAt = completed ? LocalDateTime.now() : null;
        this.progressNote = progressNote == null ? "" : progressNote;
        this.updatedByTeacher = updatedByTeacher;
    }

    public Long getId() {
        return super.getId();
    }

    public GoalTask getGoalTask() {
        return goalTask;
    }

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public String getOccurrenceLabel() {
        return occurrenceLabel;
    }

    public LocalDate getOccurrenceStartAt() {
        return occurrenceStartAt;
    }

    public LocalDate getOccurrenceEndAt() {
        return occurrenceEndAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }
}
