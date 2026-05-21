package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.time.LocalDateTime;

public class GraduationApplicationPortalCredentialDto {
    private Long studentId;
    private Long universityId;
    private String universityName;
    private String schoolAccount;
    private String schoolEmail;
    private String schoolPassword;
    private String defaultSchoolEmail;
    private String defaultSchoolPassword;
    private boolean studentVisible;
    private boolean interviewRequired;
    private boolean languageScoreRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getSchoolAccount() {
        return schoolAccount;
    }

    public void setSchoolAccount(String schoolAccount) {
        this.schoolAccount = schoolAccount;
    }

    public String getSchoolEmail() {
        return schoolEmail;
    }

    public void setSchoolEmail(String schoolEmail) {
        this.schoolEmail = schoolEmail;
    }

    public String getSchoolPassword() {
        return schoolPassword;
    }

    public void setSchoolPassword(String schoolPassword) {
        this.schoolPassword = schoolPassword;
    }

    public String getDefaultSchoolEmail() {
        return defaultSchoolEmail;
    }

    public void setDefaultSchoolEmail(String defaultSchoolEmail) {
        this.defaultSchoolEmail = defaultSchoolEmail;
    }

    public String getDefaultSchoolPassword() {
        return defaultSchoolPassword;
    }

    public void setDefaultSchoolPassword(String defaultSchoolPassword) {
        this.defaultSchoolPassword = defaultSchoolPassword;
    }

    public boolean isStudentVisible() {
        return studentVisible;
    }

    public void setStudentVisible(boolean studentVisible) {
        this.studentVisible = studentVisible;
    }

    public boolean isInterviewRequired() {
        return interviewRequired;
    }

    public void setInterviewRequired(boolean interviewRequired) {
        this.interviewRequired = interviewRequired;
    }

    public boolean isLanguageScoreRequired() {
        return languageScoreRequired;
    }

    public void setLanguageScoreRequired(boolean languageScoreRequired) {
        this.languageScoreRequired = languageScoreRequired;
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
