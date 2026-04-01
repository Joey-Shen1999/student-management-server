package com.studentmanagement.studentmanagementserver.domain.ielts;

public class IeltsSummarySnapshotDto {
    private String trackingStatus;
    private String languageTrackingStatus;

    public IeltsSummarySnapshotDto(String trackingStatus, String languageTrackingStatus) {
        this.trackingStatus = trackingStatus;
        this.languageTrackingStatus = languageTrackingStatus;
    }

    public String getTrackingStatus() {
        return trackingStatus;
    }

    public String getLanguageTrackingStatus() {
        return languageTrackingStatus;
    }
}
