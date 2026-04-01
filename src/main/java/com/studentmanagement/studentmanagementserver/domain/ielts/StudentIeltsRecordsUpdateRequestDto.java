package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class StudentIeltsRecordsUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    private String languageTrackingManualStatus;
    private List<StudentIeltsRecordDto> records;
    private boolean languageTrackingManualStatusPresent;

    public Boolean getHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public void setHasTakenIeltsAcademic(Boolean hasTakenIeltsAcademic) {
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
    }

    public String getLanguageTrackingManualStatus() {
        return languageTrackingManualStatus;
    }

    @JsonSetter("languageTrackingManualStatus")
    public void setLanguageTrackingManualStatus(String languageTrackingManualStatus) {
        this.languageTrackingManualStatus = languageTrackingManualStatus;
        this.languageTrackingManualStatusPresent = true;
    }

    public List<StudentIeltsRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<StudentIeltsRecordDto> records) {
        this.records = records;
    }

    @JsonIgnore
    public boolean isLanguageTrackingManualStatusPresent() {
        return languageTrackingManualStatusPresent;
    }
}
