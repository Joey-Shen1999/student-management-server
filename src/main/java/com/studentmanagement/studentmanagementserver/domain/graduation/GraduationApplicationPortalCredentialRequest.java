package com.studentmanagement.studentmanagementserver.domain.graduation;

public class GraduationApplicationPortalCredentialRequest {
    private String schoolAccount;
    private String schoolEmail;
    private String schoolPassword;
    private Boolean studentVisible;
    private Boolean interviewRequired;
    private Boolean languageScoreRequired;

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

    public Boolean getStudentVisible() {
        return studentVisible;
    }

    public void setStudentVisible(Boolean studentVisible) {
        this.studentVisible = studentVisible;
    }

    public Boolean getInterviewRequired() {
        return interviewRequired;
    }

    public void setInterviewRequired(Boolean interviewRequired) {
        this.interviewRequired = interviewRequired;
    }

    public Boolean getLanguageScoreRequired() {
        return languageScoreRequired;
    }

    public void setLanguageScoreRequired(Boolean languageScoreRequired) {
        this.languageScoreRequired = languageScoreRequired;
    }
}
