package com.studentmanagement.studentmanagementserver.domain.student;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class StudentSchoolTranscriptDto {

    private Long schoolRecordId;
    private String transcriptFileName;
    private String transcriptContentType;
    private Long transcriptSizeBytes;
    private String transcriptUploadedAt;
    private String academicRecordType;
    private Integer reportYear;
    private String reportMonth;
    private Boolean hasTranscript;
    private List<TranscriptItemDto> transcripts;

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

    public String getAcademicRecordType() {
        return academicRecordType;
    }

    public void setAcademicRecordType(String academicRecordType) {
        this.academicRecordType = academicRecordType;
    }

    public Integer getReportYear() {
        return reportYear;
    }

    public void setReportYear(Integer reportYear) {
        this.reportYear = reportYear;
    }

    public String getReportMonth() {
        return reportMonth;
    }

    public void setReportMonth(String reportMonth) {
        this.reportMonth = reportMonth;
    }

    public Boolean getHasTranscript() {
        return hasTranscript;
    }

    public void setHasTranscript(Boolean hasTranscript) {
        this.hasTranscript = hasTranscript;
    }

    public List<TranscriptItemDto> getTranscripts() {
        return transcripts;
    }

    public void setTranscripts(List<TranscriptItemDto> transcripts) {
        this.transcripts = transcripts;
    }

    @JsonIgnore
    public List<TranscriptItemDto> getTranscriptsOrEmpty() {
        return transcripts == null ? new ArrayList<TranscriptItemDto>() : transcripts;
    }

    public static class TranscriptItemDto {
        private Long id;
        private String transcriptFileName;
        private String transcriptContentType;
        private Long transcriptSizeBytes;
        private String transcriptUploadedAt;
        private Long uploadedBy;
        private String academicRecordType;
        private Integer reportYear;
        private String reportMonth;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Long getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(Long uploadedBy) {
            this.uploadedBy = uploadedBy;
        }

        public String getAcademicRecordType() {
            return academicRecordType;
        }

        public void setAcademicRecordType(String academicRecordType) {
            this.academicRecordType = academicRecordType;
        }

        public Integer getReportYear() {
            return reportYear;
        }

        public void setReportYear(Integer reportYear) {
            this.reportYear = reportYear;
        }

        public String getReportMonth() {
            return reportMonth;
        }

        public void setReportMonth(String reportMonth) {
            this.reportMonth = reportMonth;
        }
    }
}
