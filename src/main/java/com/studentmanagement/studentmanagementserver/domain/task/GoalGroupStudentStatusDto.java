package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class GoalGroupStudentStatusDto {
    private Long goalId;
    private String taskGroupId;
    private Long studentId;
    private String studentName;
    private String username;
    private String email;
    private GoalTaskStatus status;
    private boolean completed;
    private boolean active;
    private String enrollmentStartAt;
    private String enrollmentEndAt;
    private int completedOccurrences;
    private int totalOccurrences;
    private String completedAt;
    private String updatedAt;
    private String progressNote;
    private List<GoalTaskCompletionDto> completions;

    public GoalGroupStudentStatusDto(Long goalId,
                                     String taskGroupId,
                                     Long studentId,
                                     String studentName,
                                     String username,
                                     String email,
                                     GoalTaskStatus status,
                                     boolean completed,
                                     boolean active,
                                     String enrollmentStartAt,
                                     String enrollmentEndAt,
                                     int completedOccurrences,
                                     int totalOccurrences,
                                     String completedAt,
                                     String updatedAt,
                                     String progressNote,
                                     List<GoalTaskCompletionDto> completions) {
        this.goalId = goalId;
        this.taskGroupId = taskGroupId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.username = username;
        this.email = email;
        this.status = status;
        this.completed = completed;
        this.active = active;
        this.enrollmentStartAt = enrollmentStartAt;
        this.enrollmentEndAt = enrollmentEndAt;
        this.completedOccurrences = completedOccurrences;
        this.totalOccurrences = totalOccurrences;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
        this.progressNote = progressNote;
        this.completions = completions;
    }

    public Long getGoalId() {
        return goalId;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public GoalTaskStatus getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isActive() {
        return active;
    }

    public String getEnrollmentStartAt() {
        return enrollmentStartAt;
    }

    public String getEnrollmentEndAt() {
        return enrollmentEndAt;
    }

    public int getCompletedOccurrences() {
        return completedOccurrences;
    }

    public int getTotalOccurrences() {
        return totalOccurrences;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }

    public List<GoalTaskCompletionDto> getCompletions() {
        return completions;
    }
}
