package com.studentmanagement.studentmanagementserver.domain.ielts;

public class StudentIeltsPreparationIntentUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    private String preparationIntent;

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
}
