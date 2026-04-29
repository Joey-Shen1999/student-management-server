package com.studentmanagement.studentmanagementserver.domain.task;

public class GoalGroupStudentStatusDto {
    private Long goalId;
    private String taskGroupId;
    private Long studentId;
    private String studentName;
    private String username;
    private String email;
    private GoalTaskStatus status;
    private boolean completed;
    private String completedAt;
    private String updatedAt;
    private String progressNote;

    public GoalGroupStudentStatusDto(Long goalId,
                                     String taskGroupId,
                                     Long studentId,
                                     String studentName,
                                     String username,
                                     String email,
                                     GoalTaskStatus status,
                                     boolean completed,
                                     String completedAt,
                                     String updatedAt,
                                     String progressNote) {
        this.goalId = goalId;
        this.taskGroupId = taskGroupId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.username = username;
        this.email = email;
        this.status = status;
        this.completed = completed;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
        this.progressNote = progressNote;
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

    public String getCompletedAt() {
        return completedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }
}
