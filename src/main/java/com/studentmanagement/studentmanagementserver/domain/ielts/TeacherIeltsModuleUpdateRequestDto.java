package com.studentmanagement.studentmanagementserver.domain.ielts;

import java.util.List;

public class TeacherIeltsModuleUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private List<StudentIeltsRecordDto> records;

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

    public List<StudentIeltsRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<StudentIeltsRecordDto> records) {
        this.records = records;
    }
}
