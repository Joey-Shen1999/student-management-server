package com.studentmanagement.studentmanagementserver.domain.task;

public class InstantiateDllTemplateRequestDto {
    private Long assignedStudentId;
    private String title;
    private String status;

    public Long getAssignedStudentId() {
        return assignedStudentId;
    }

    public void setAssignedStudentId(Long assignedStudentId) {
        this.assignedStudentId = assignedStudentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
