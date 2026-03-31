package com.studentmanagement.studentmanagementserver.domain.ielts;

import java.util.List;

public class StudentIeltsRecordsUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    private List<StudentIeltsRecordDto> records;

    public Boolean getHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public void setHasTakenIeltsAcademic(Boolean hasTakenIeltsAcademic) {
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
    }

    public List<StudentIeltsRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<StudentIeltsRecordDto> records) {
        this.records = records;
    }
}
