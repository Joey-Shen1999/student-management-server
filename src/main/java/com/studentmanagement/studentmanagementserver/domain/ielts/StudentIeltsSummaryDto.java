package com.studentmanagement.studentmanagementserver.domain.ielts;

public class StudentIeltsSummaryDto {
    private Long studentId;
    private boolean shouldShowIeltsModule;
    private boolean hasTakenIeltsAcademic;
    private String preparationIntent;
    private Integer recordCount;
    private String latestTestDate;
    private Double bestOverallBand;
    private String updatedAt;

    public StudentIeltsSummaryDto(Long studentId,
                                  boolean shouldShowIeltsModule,
                                  boolean hasTakenIeltsAcademic,
                                  String preparationIntent,
                                  Integer recordCount,
                                  String latestTestDate,
                                  Double bestOverallBand,
                                  String updatedAt) {
        this.studentId = studentId;
        this.shouldShowIeltsModule = shouldShowIeltsModule;
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
        this.preparationIntent = preparationIntent;
        this.recordCount = recordCount;
        this.latestTestDate = latestTestDate;
        this.bestOverallBand = bestOverallBand;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
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
}
