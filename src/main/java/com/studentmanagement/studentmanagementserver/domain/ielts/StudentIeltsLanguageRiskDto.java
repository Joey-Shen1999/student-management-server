package com.studentmanagement.studentmanagementserver.domain.ielts;

import java.util.List;

public class StudentIeltsLanguageRiskDto {
    private boolean shouldShowIeltsModule;
    private String languageRiskFlag;
    private String firstLanguage;
    private String citizenship;
    private Integer canadaStudyYears;
    private boolean hasCanadianHighSchoolExperience;
    private String profileCompleteness;
    private List<String> riskReasonCodes;

    public StudentIeltsLanguageRiskDto(boolean shouldShowIeltsModule,
                                       String languageRiskFlag,
                                       String firstLanguage,
                                       String citizenship,
                                       Integer canadaStudyYears,
                                       boolean hasCanadianHighSchoolExperience,
                                       String profileCompleteness,
                                       List<String> riskReasonCodes) {
        this.shouldShowIeltsModule = shouldShowIeltsModule;
        this.languageRiskFlag = languageRiskFlag;
        this.firstLanguage = firstLanguage;
        this.citizenship = citizenship;
        this.canadaStudyYears = canadaStudyYears;
        this.hasCanadianHighSchoolExperience = hasCanadianHighSchoolExperience;
        this.profileCompleteness = profileCompleteness;
        this.riskReasonCodes = riskReasonCodes;
    }

    public boolean isShouldShowIeltsModule() {
        return shouldShowIeltsModule;
    }

    public String getLanguageRiskFlag() {
        return languageRiskFlag;
    }

    public String getFirstLanguage() {
        return firstLanguage;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public Integer getCanadaStudyYears() {
        return canadaStudyYears;
    }

    public boolean isHasCanadianHighSchoolExperience() {
        return hasCanadianHighSchoolExperience;
    }

    public String getProfileCompleteness() {
        return profileCompleteness;
    }

    public List<String> getRiskReasonCodes() {
        return riskReasonCodes;
    }
}
