package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class StudentIeltsPreparationIntentUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    @JsonAlias({"testType", "test_type"})
    private String languageScoreType;
    private String preparationIntent;
    private String languageScoreTrackingManualStatus;
    private boolean languageScoreTrackingManualStatusPresent;

    public Boolean getHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public void setHasTakenIeltsAcademic(Boolean hasTakenIeltsAcademic) {
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
    }

    public String getLanguageScoreType() {
        return languageScoreType;
    }

    public void setLanguageScoreType(String languageScoreType) {
        this.languageScoreType = languageScoreType;
    }

    public String getPreparationIntent() {
        return preparationIntent;
    }

    public void setPreparationIntent(String preparationIntent) {
        this.preparationIntent = preparationIntent;
    }

    public String getLanguageScoreTrackingManualStatus() {
        return languageScoreTrackingManualStatus;
    }

    public String getLanguageTrackingManualStatus() {
        return languageScoreTrackingManualStatus;
    }

    @JsonSetter("languageScoreTrackingManualStatus")
    public void setLanguageScoreTrackingManualStatus(String languageScoreTrackingManualStatus) {
        this.languageScoreTrackingManualStatus = languageScoreTrackingManualStatus;
        this.languageScoreTrackingManualStatusPresent = true;
    }

    @JsonSetter("languageTrackingManualStatus")
    public void setLanguageTrackingManualStatus(String languageTrackingManualStatus) {
        setLanguageScoreTrackingManualStatus(languageTrackingManualStatus);
    }

    @JsonIgnore
    public boolean isLanguageScoreTrackingManualStatusPresent() {
        return languageScoreTrackingManualStatusPresent;
    }

    @JsonIgnore
    public boolean isLanguageTrackingManualStatusPresent() {
        return languageScoreTrackingManualStatusPresent;
    }
}
