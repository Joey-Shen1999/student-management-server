package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class StudentIeltsRecordsUpdateRequestDto {
    private Boolean hasTakenIeltsAcademic;
    @JsonAlias({"testType", "test_type"})
    private String languageScoreType;
    private String languageScoreTrackingManualStatus;
    private List<StudentIeltsRecordDto> records;
    private List<StudentIeltsRecordDto> toeflRecords;
    private List<StudentIeltsRecordDto> duolingoRecords;
    private boolean languageScoreTrackingManualStatusPresent;
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
    public boolean isLanguageScoreTrackingManualStatusPresent() {
        return languageScoreTrackingManualStatusPresent;
    }

    @JsonIgnore
    public boolean isLanguageTrackingManualStatusPresent() {
        return languageScoreTrackingManualStatusPresent;
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
