package com.studentmanagement.studentmanagementserver.domain.task;

public class GoalTaskDto {
    private Long id;
    private String type;
    private String title;
    private String description;
    private GoalTaskStatus status;
    private String dueAt;
    private String taskGroupId;
    private String cycleType;
    private String cycleFrequency;
    private Integer cycleInterval;
    private String cycleUnit;
    private String cycleLabel;
    private String cycleEndAt;
    private boolean cycleNoEnd;
    private String enrollmentStartAt;
    private String enrollmentEndAt;
    private boolean active;
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
                       String cycleType,
                       String cycleFrequency,
                       Integer cycleInterval,
                       String cycleUnit,
                       String cycleLabel,
                       String cycleEndAt,
                       boolean cycleNoEnd,
                       String enrollmentStartAt,
                       String enrollmentEndAt,
                       boolean active,
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
        this.cycleType = cycleType;
        this.cycleFrequency = cycleFrequency;
        this.cycleInterval = cycleInterval;
        this.cycleUnit = cycleUnit;
        this.cycleLabel = cycleLabel;
        this.cycleEndAt = cycleEndAt;
        this.cycleNoEnd = cycleNoEnd;
        this.enrollmentStartAt = enrollmentStartAt;
        this.enrollmentEndAt = enrollmentEndAt;
        this.active = active;
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

    public String getCycleEndAt() {
        return cycleEndAt;
    }

    public boolean isCycleNoEnd() {
        return cycleNoEnd;
    }

    public String getEnrollmentStartAt() {
        return enrollmentStartAt;
    }

    public String getEnrollmentEndAt() {
        return enrollmentEndAt;
    }

    public boolean isActive() {
        return active;
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
