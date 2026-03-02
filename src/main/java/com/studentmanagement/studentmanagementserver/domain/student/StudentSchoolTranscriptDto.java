package com.studentmanagement.studentmanagementserver.domain.student;

public class StudentSchoolTranscriptDto {

    private Long schoolRecordId;
    private String transcriptFileName;
    private String transcriptContentType;
    private Long transcriptSizeBytes;
    private String transcriptUploadedAt;
    private Boolean hasTranscript;

    public Long getSchoolRecordId() {
        return schoolRecordId;
    }

    public void setSchoolRecordId(Long schoolRecordId) {
        this.schoolRecordId = schoolRecordId;
    }

    public String getTranscriptFileName() {
        return transcriptFileName;
    }

    public void setTranscriptFileName(String transcriptFileName) {
        this.transcriptFileName = transcriptFileName;
    }

    public String getTranscriptContentType() {
        return transcriptContentType;
    }

    public void setTranscriptContentType(String transcriptContentType) {
        this.transcriptContentType = transcriptContentType;
    }

    public Long getTranscriptSizeBytes() {
        return transcriptSizeBytes;
    }

    public void setTranscriptSizeBytes(Long transcriptSizeBytes) {
        this.transcriptSizeBytes = transcriptSizeBytes;
    }

    public String getTranscriptUploadedAt() {
        return transcriptUploadedAt;
    }

    public void setTranscriptUploadedAt(String transcriptUploadedAt) {
        this.transcriptUploadedAt = transcriptUploadedAt;
    }

    public Boolean getHasTranscript() {
        return hasTranscript;
    }

    public void setHasTranscript(Boolean hasTranscript) {
        this.hasTranscript = hasTranscript;
    }
}
