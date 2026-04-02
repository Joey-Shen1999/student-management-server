package com.studentmanagement.studentmanagementserver.domain.ielts;

public class IeltsSummarySnapshotDto {
    private String languageScoreType;
    private String trackingStatus;
    private String languageTrackingStatus;

    public IeltsSummarySnapshotDto(String languageScoreType,
                                   String trackingStatus,
                                   String languageTrackingStatus) {
        this.languageScoreType = languageScoreType;
        this.trackingStatus = trackingStatus;
        this.languageTrackingStatus = languageTrackingStatus;
    }

    public String getLanguageScoreType() {
        return languageScoreType;
    }

    public String getTrackingStatus() {
        return trackingStatus;
    }

    public String getLanguageTrackingStatus() {
        return languageTrackingStatus;
    }
}
