package com.studentmanagement.studentmanagementserver.domain.task;

public class DllTaskDto {
    private Long id;
    private Long templateId;
    private String title;
    private DllTaskStatus status;
    private Long assignedStudentId;
    private String createdAt;

    public DllTaskDto(Long id,
                      Long templateId,
                      String title,
                      DllTaskStatus status,
                      Long assignedStudentId,
                      String createdAt) {
        this.id = id;
        this.templateId = templateId;
        this.title = title;
        this.status = status;
        this.assignedStudentId = assignedStudentId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getTitle() {
        return title;
    }

    public DllTaskStatus getStatus() {
        return status;
    }

    public Long getAssignedStudentId() {
        return assignedStudentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
