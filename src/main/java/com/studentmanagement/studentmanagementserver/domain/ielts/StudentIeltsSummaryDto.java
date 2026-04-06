package com.studentmanagement.studentmanagementserver.domain.ielts;

public class StudentIeltsSummaryDto {
    private Long studentId;
    private String languageScoreType;
    private boolean shouldShowIeltsModule;
    private boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private Integer recordCount;
    private String latestTestDate;
    private Double bestOverallBand;
    private String updatedAt;
    // Deprecated compatibility fields; target removal date: 2026-05-31.
    private String trackingStatus;
    // Deprecated compatibility fields; target removal date: 2026-05-31.
    private String languageScoreTrackingStatus;
    private IeltsSummarySnapshotDto summary;

    public StudentIeltsSummaryDto(Long studentId,
                                  String languageScoreType,
                                  boolean shouldShowIeltsModule,
                                  boolean hasTakenIeltsAcademic,
                                  String preparationIntent,
                                  Integer recordCount,
                                  String latestTestDate,
                                  Double bestOverallBand,
                                  String updatedAt,
                                  String trackingStatus,
                                  String languageScoreTrackingStatus,
                                  IeltsSummarySnapshotDto summary) {
        this.studentId = studentId;
        this.languageScoreType = languageScoreType;
        this.shouldShowIeltsModule = shouldShowIeltsModule;
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
        this.preparationIntent = preparationIntent;
        this.recordCount = recordCount;
        this.latestTestDate = latestTestDate;
        this.bestOverallBand = bestOverallBand;
        this.updatedAt = updatedAt;
        this.trackingStatus = trackingStatus;
        this.languageScoreTrackingStatus = languageScoreTrackingStatus;
        this.summary = summary;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getLanguageScoreType() {
        return languageScoreType;
    }

    public boolean isShouldShowIeltsModule() {
        return shouldShowIeltsModule;
    }

    public boolean isHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public String getPreparationIntent() {
        return preparationIntent;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public String getLatestTestDate() {
        return latestTestDate;
    }

    public Double getBestOverallBand() {
        return bestOverallBand;
    }

    public String getUpdatedAt() {
        return updatedAt;
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
}
