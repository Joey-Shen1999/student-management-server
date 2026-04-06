package com.studentmanagement.studentmanagementserver.domain.ielts;

public class IeltsSummarySnapshotDto {
    private String languageScoreType;
    private String trackingStatus;
    private String languageScoreTrackingStatus;

    public IeltsSummarySnapshotDto(String languageScoreType,
                                   String trackingStatus,
                                   String languageScoreTrackingStatus) {
        this.languageScoreType = languageScoreType;
        this.trackingStatus = trackingStatus;
        this.languageScoreTrackingStatus = languageScoreTrackingStatus;
    }

    public String getLanguageScoreType() {
        return languageScoreType;
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
}
