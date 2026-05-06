package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import java.time.LocalDate;

public class ExtracurricularActivityDto {
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

    public ExtracurricularActivityDto(String activityType,
                                      String activityName,
                                      String organization,
                                      String role,
                                      String activityLevel,
                                      String awardOrResult,
                                      String competitionCategory,
                                      LocalDate activityDate,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      String description,
                                      String admissionRelevance,
                                      String proofContact,
                                      String proofUrl) {
        this.activityType = activityType;
        this.activityName = activityName;
        this.organization = organization;
        this.role = role;
        this.activityLevel = activityLevel;
        this.awardOrResult = awardOrResult;
        this.competitionCategory = competitionCategory;
        this.activityDate = activityDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.admissionRelevance = admissionRelevance;
        this.proofContact = proofContact;
        this.proofUrl = proofUrl;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getRole() {
        return role;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String getAwardOrResult() {
        return awardOrResult;
    }

    public String getCompetitionCategory() {
        return competitionCategory;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public String getAdmissionRelevance() {
        return admissionRelevance;
    }

    public String getProofContact() {
        return proofContact;
    }

    public String getProofUrl() {
        return proofUrl;
    }
}
