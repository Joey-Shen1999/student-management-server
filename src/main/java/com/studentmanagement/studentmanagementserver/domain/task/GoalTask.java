package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "goal_tasks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_goal_tasks_group_student",
                        columnNames = {"task_group_id", "assigned_student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_goal_tasks_assigned_student", columnList = "assigned_student_id"),
                @Index(name = "idx_goal_tasks_assigned_teacher", columnList = "assigned_by_teacher_id"),
                @Index(name = "idx_goal_tasks_status", columnList = "status"),
                @Index(name = "idx_goal_tasks_due_at", columnList = "due_at"),
                @Index(name = "idx_goal_tasks_task_group_id", columnList = "task_group_id"),
                @Index(name = "idx_goal_tasks_student_updated_id", columnList = "assigned_student_id,updatedAt,id"),
                @Index(name = "idx_goal_tasks_teacher_updated_id", columnList = "assigned_by_teacher_id,updatedAt,id")
        }
)
public class GoalTask extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalTaskStatus status;

    @Column(name = "due_at")
    private LocalDate dueAt;

    @Column(name = "cycle_type", nullable = false, length = 20)
    private String cycleType;

    @Column(name = "cycle_frequency", length = 20)
    private String cycleFrequency;

    @Column(name = "cycle_interval")
    private Integer cycleInterval;

    @Column(name = "cycle_unit", length = 20)
    private String cycleUnit;

    @Column(name = "cycle_label", length = 120)
    private String cycleLabel;

    @Column(name = "cycle_end_at")
    private LocalDate cycleEndAt;

    @Column(name = "cycle_no_end", nullable = false)
    private boolean cycleNoEnd;

    @Column(name = "enrollment_start_at")
    private LocalDate enrollmentStartAt;

    @Column(name = "enrollment_end_at")
    private LocalDate enrollmentEndAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "task_group_id", nullable = false, length = 64)
    private String taskGroupId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_student_id", nullable = false)
    private Student assignedStudent;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_teacher_id", nullable = false)
    private Teacher assignedByTeacher;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress_note", nullable = false, length = 2000)
    private String progressNote;

    protected GoalTask() {
    }

    public GoalTask(String title,
                    String description,
                    LocalDate dueAt,
                    Student assignedStudent,
                    Teacher assignedByTeacher) {
        this(title, description, dueAt, assignedStudent, assignedByTeacher, null);
    }

    public GoalTask(String title,
                    String description,
                    LocalDate dueAt,
                    Student assignedStudent,
                    Teacher assignedByTeacher,
                    String taskGroupId) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.taskGroupId = taskGroupId;
        this.assignedStudent = assignedStudent;
        this.assignedByTeacher = assignedByTeacher;
        this.status = GoalTaskStatus.NOT_STARTED;
        this.progressNote = "";
        this.cycleType = "ONE_TIME";
        this.cycleNoEnd = true;
        this.active = true;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.status == null) {
            this.status = GoalTaskStatus.NOT_STARTED;
        }
        if (this.progressNote == null) {
            this.progressNote = "";
        }
        if (this.cycleType == null || this.cycleType.trim().isEmpty()) {
            this.cycleType = "ONE_TIME";
        }
        if (this.status == GoalTaskStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
        if (this.enrollmentStartAt == null) {
            this.enrollmentStartAt = this.dueAt;
        }
        if (this.taskGroupId == null || this.taskGroupId.trim().isEmpty()) {
            this.taskGroupId = "auto-" + UUID.randomUUID().toString();
        }
    }

    public void updateStatus(GoalTaskStatus nextStatus, String nextProgressNote, boolean overwriteProgressNote) {
        this.status = nextStatus;
        if (nextStatus == GoalTaskStatus.COMPLETED) {
            if (this.completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
        } else {
            this.completedAt = null;
        }
        if (overwriteProgressNote) {
            this.progressNote = nextProgressNote == null ? "" : nextProgressNote;
        }
    }

    public void updateGoal(String title, String description, LocalDate dueAt, Student assignedStudent) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.assignedStudent = assignedStudent;
    }

    public void updateCycle(String cycleType,
                            String cycleFrequency,
                            Integer cycleInterval,
                            String cycleUnit,
                            String cycleLabel,
                            LocalDate cycleEndAt,
                            boolean cycleNoEnd) {
        this.cycleType = cycleType == null || cycleType.trim().isEmpty() ? "ONE_TIME" : cycleType;
        this.cycleFrequency = cycleFrequency;
        this.cycleInterval = cycleInterval;
        this.cycleUnit = cycleUnit;
        this.cycleLabel = cycleLabel;
        this.cycleEndAt = cycleEndAt;
        this.cycleNoEnd = cycleNoEnd;
    }

    public void activate(LocalDate enrollmentStartAt) {
        this.active = true;
        this.enrollmentEndAt = null;
        if (this.enrollmentStartAt == null) {
            this.enrollmentStartAt = enrollmentStartAt;
        }
    }

    public void deactivate(LocalDate enrollmentEndAt) {
        this.active = false;
        this.enrollmentEndAt = enrollmentEndAt;
    }

    public Long getId() {
        return super.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public GoalTaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueAt() {
        return dueAt;
    }

    public String getCycleType() {
        return cycleType;
    }

    public String getCycleFrequency() {
        return cycleFrequency;
    }

    public Integer getCycleInterval() {
        return cycleInterval;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public String getCycleLabel() {
        return cycleLabel;
    }

    public LocalDate getCycleEndAt() {
        return cycleEndAt;
    }

    public boolean isCycleNoEnd() {
        return cycleNoEnd;
    }

    public LocalDate getEnrollmentStartAt() {
        return enrollmentStartAt;
    }

    public LocalDate getEnrollmentEndAt() {
        return enrollmentEndAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public Student getAssignedStudent() {
        return assignedStudent;
    }

    public Teacher getAssignedByTeacher() {
        return assignedByTeacher;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }
}
