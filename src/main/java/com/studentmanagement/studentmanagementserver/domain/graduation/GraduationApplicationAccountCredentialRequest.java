package com.studentmanagement.studentmanagementserver.domain.graduation;

public class GraduationApplicationAccountCredentialRequest {
    private String applicationEmail;
    private String applicationPassword;

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
}
