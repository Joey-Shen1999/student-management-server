package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class TeacherIeltsModuleUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    @JsonAlias({"testType", "test_type"})
    private String languageScoreType;
    private String preparationIntent;
    private String languageTrackingManualStatus;
    private List<StudentIeltsRecordDto> records;
    private List<StudentIeltsRecordDto> toeflRecords;
    private List<StudentIeltsRecordDto> duolingoRecords;
    private boolean languageTrackingManualStatusPresent;
    private boolean toeflRecordsPresent;
    private boolean duolingoRecordsPresent;

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

    public List<StudentIeltsRecordDto> getToeflRecords() {
        return toeflRecords;
    }

    @JsonSetter("toeflRecords")
    public void setToeflRecords(List<StudentIeltsRecordDto> toeflRecords) {
        this.toeflRecords = toeflRecords;
        this.toeflRecordsPresent = true;
    }

    public List<StudentIeltsRecordDto> getDuolingoRecords() {
        return duolingoRecords;
    }

    @JsonSetter("duolingoRecords")
    public void setDuolingoRecords(List<StudentIeltsRecordDto> duolingoRecords) {
        this.duolingoRecords = duolingoRecords;
        this.duolingoRecordsPresent = true;
    }

    @JsonIgnore
    public boolean isLanguageTrackingManualStatusPresent() {
        return languageTrackingManualStatusPresent;
    }

    @JsonIgnore
    public boolean isToeflRecordsPresent() {
        return toeflRecordsPresent;
    }

    @JsonIgnore
    public boolean isDuolingoRecordsPresent() {
        return duolingoRecordsPresent;
    }
}
