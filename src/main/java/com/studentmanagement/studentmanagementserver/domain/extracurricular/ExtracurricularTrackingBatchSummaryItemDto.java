package com.studentmanagement.studentmanagementserver.domain.extracurricular;

public class ExtracurricularTrackingBatchSummaryItemDto {
    private Long studentId;
    private int totalActivities;
    private int competitionCount;
    private int awardCount;
    private String updatedAt;

    public ExtracurricularTrackingBatchSummaryItemDto(Long studentId,
                                                      int totalActivities,
                                                      int competitionCount,
                                                      int awardCount,
                                                      String updatedAt) {
        this.studentId = studentId;
        this.totalActivities = totalActivities;
        this.competitionCount = competitionCount;
        this.awardCount = awardCount;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public int getCompetitionCount() {
        return competitionCount;
    }

    public int getAwardCount() {
        return awardCount;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
