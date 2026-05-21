package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDateTime;

public class GraduationApplicationAccountCredentialDto {
    private Long studentId;
    private String applicationEmail;
    private String applicationPassword;
    private String defaultApplicationEmail;
    private String defaultApplicationPassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getApplicationEmail() {
        return applicationEmail;
    }

    public void setApplicationEmail(String applicationEmail) {
        this.applicationEmail = applicationEmail;
    }

    public String getApplicationPassword() {
        return applicationPassword;
    }

    public void setApplicationPassword(String applicationPassword) {
        this.applicationPassword = applicationPassword;
    }

    public String getDefaultApplicationEmail() {
        return defaultApplicationEmail;
    }

    public void setDefaultApplicationEmail(String defaultApplicationEmail) {
        this.defaultApplicationEmail = defaultApplicationEmail;
    }

    public String getDefaultApplicationPassword() {
        return defaultApplicationPassword;
    }

    public void setDefaultApplicationPassword(String defaultApplicationPassword) {
        this.defaultApplicationPassword = defaultApplicationPassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
