package com.studentmanagement.studentmanagementserver.domain.graduation;

public class GraduationApplicationPortalCredentialRequest {
    private String schoolAccount;
    private String schoolEmail;
    private String schoolPassword;

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
}
