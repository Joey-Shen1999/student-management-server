package com.studentmanagement.studentmanagementserver.domain.ielts;

import java.util.List;

public class StudentIeltsModuleStateDto {
    private Long studentId;
    private Integer graduationYear;
    private String languageScoreType;
    private boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private String languageScoreTrackingManualStatus;
    private String trackingStatus;
    private String languageScoreTrackingStatus;
    private IeltsSummarySnapshotDto summary;
    private List<StudentIeltsRecordDto> records;
    private StudentIeltsLanguageRiskDto languageRisk;
    private String updatedAt;

    public StudentIeltsModuleStateDto(Long studentId,
                                      Integer graduationYear,
                                      String languageScoreType,
                                      boolean hasTakenIeltsAcademic,
                                      String preparationIntent,
                                      String languageScoreTrackingManualStatus,
                                      String trackingStatus,
                                      String languageScoreTrackingStatus,
                                      IeltsSummarySnapshotDto summary,
                                      List<StudentIeltsRecordDto> records,
                                      StudentIeltsLanguageRiskDto languageRisk,
                                      String updatedAt) {
        this.studentId = studentId;
        this.graduationYear = graduationYear;
        this.languageScoreType = languageScoreType;
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
        this.preparationIntent = preparationIntent;
        this.languageScoreTrackingManualStatus = languageScoreTrackingManualStatus;
        this.trackingStatus = trackingStatus;
        this.languageScoreTrackingStatus = languageScoreTrackingStatus;
        this.summary = summary;
        this.records = records;
        this.languageRisk = languageRisk;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public String getLanguageScoreType() {
        return languageScoreType;
    }

    public boolean isHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public String getPreparationIntent() {
        return preparationIntent;
    }

    public String getLanguageScoreTrackingManualStatus() {
        return languageScoreTrackingManualStatus;
    }

    public String getLanguageTrackingManualStatus() {
        return languageScoreTrackingManualStatus;
    }

    public String getTrackingStatus() {
        return trackingStatus;
    }

    public String getLanguageScoreTrackingStatus() {
        return languageScoreTrackingStatus;
    }

    public String getLanguageTrackingStatus() {
        return languageScoreTrackingStatus;
    }

    public IeltsSummarySnapshotDto getSummary() {
        return summary;
    }

    public List<StudentIeltsRecordDto> getRecords() {
        return records;
    }

    public StudentIeltsLanguageRiskDto getLanguageRisk() {
        return languageRisk;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
