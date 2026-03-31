package com.studentmanagement.studentmanagementserver.domain.task;

public class GoalTaskDto {
    private Long id;
    private String type;
    private String title;
    private String description;
    private GoalTaskStatus status;
    private String dueAt;
    private String taskGroupId;
    private Long assignedStudentId;
    private String assignedStudentName;
    private Long assignedByTeacherId;
    private String assignedByTeacherName;
    private String createdAt;
    private String updatedAt;
    private String completedAt;
    private String progressNote;

    public GoalTaskDto(Long id,
                       String type,
                       String title,
                       String description,
                       GoalTaskStatus status,
                       String dueAt,
                       String taskGroupId,
                       Long assignedStudentId,
                       String assignedStudentName,
                       Long assignedByTeacherId,
                       String assignedByTeacherName,
                       String createdAt,
                       String updatedAt,
                       String completedAt,
                       String progressNote) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueAt = dueAt;
        this.taskGroupId = taskGroupId;
        this.assignedStudentId = assignedStudentId;
        this.assignedStudentName = assignedStudentName;
        this.assignedByTeacherId = assignedByTeacherId;
        this.assignedByTeacherName = assignedByTeacherName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.progressNote = progressNote;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
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

    public String getDueAt() {
        return dueAt;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public Long getAssignedStudentId() {
        return assignedStudentId;
    }

    public String getAssignedStudentName() {
        return assignedStudentName;
    }

    public Long getAssignedByTeacherId() {
        return assignedByTeacherId;
    }

    public String getAssignedByTeacherName() {
        return assignedByTeacherName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }
}
