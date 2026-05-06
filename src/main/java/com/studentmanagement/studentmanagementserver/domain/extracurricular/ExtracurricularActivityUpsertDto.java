package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import java.time.LocalDate;

public class ExtracurricularActivityUpsertDto {
    private String activityType;
    private String activityName;
    private String organization;
    private String role;
    private String activityLevel;
    private String awardOrResult;
    private String competitionCategory;
    private LocalDate activityDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String admissionRelevance;
    private String proofContact;
    private String proofUrl;

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getAwardOrResult() {
        return awardOrResult;
    }

    public void setAwardOrResult(String awardOrResult) {
        this.awardOrResult = awardOrResult;
    }

    public String getCompetitionCategory() {
        return competitionCategory;
    }

    public void setCompetitionCategory(String competitionCategory) {
        this.competitionCategory = competitionCategory;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdmissionRelevance() {
        return admissionRelevance;
    }

    public void setAdmissionRelevance(String admissionRelevance) {
        this.admissionRelevance = admissionRelevance;
    }

    public String getProofContact() {
        return proofContact;
    }

    public void setProofContact(String proofContact) {
        this.proofContact = proofContact;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }
}
