package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class StudentIeltsPreparationIntentUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private String languageTrackingManualStatus;
    private boolean languageTrackingManualStatusPresent;

    public Boolean getHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public void setHasTakenIeltsAcademic(Boolean hasTakenIeltsAcademic) {
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
    }

    public String getPreparationIntent() {
        return preparationIntent;
    }

    public void setPreparationIntent(String preparationIntent) {
        this.preparationIntent = preparationIntent;
    }

    public String getLanguageTrackingManualStatus() {
        return languageTrackingManualStatus;
    }

    @JsonSetter("languageTrackingManualStatus")
    public void setLanguageTrackingManualStatus(String languageTrackingManualStatus) {
        this.languageTrackingManualStatus = languageTrackingManualStatus;
        this.languageTrackingManualStatusPresent = true;
    }

    @JsonIgnore
    public boolean isLanguageTrackingManualStatusPresent() {
        return languageTrackingManualStatusPresent;
    }
}
