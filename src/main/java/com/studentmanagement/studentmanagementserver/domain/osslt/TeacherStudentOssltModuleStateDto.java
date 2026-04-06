package com.studentmanagement.studentmanagementserver.domain.osslt;

public class TeacherStudentOssltModuleStateDto {
    private Long studentId;
    private Integer graduationYear;
    private String latestOssltResult;
    private String latestOssltDate;
    private Boolean hasOsslc;
    private String ossltTrackingManualStatus;
    private String ossltTrackingStatus;
    private String updatedAt;

    public TeacherStudentOssltModuleStateDto(Long studentId,
                                             Integer graduationYear,
                                             String latestOssltResult,
                                             String latestOssltDate,
                                             Boolean hasOsslc,
                                             String ossltTrackingManualStatus,
                                             String ossltTrackingStatus,
                                             String updatedAt) {
        this.studentId = studentId;
        this.graduationYear = graduationYear;
        this.latestOssltResult = latestOssltResult;
        this.latestOssltDate = latestOssltDate;
        this.hasOsslc = hasOsslc;
        this.ossltTrackingManualStatus = ossltTrackingManualStatus;
        this.ossltTrackingStatus = ossltTrackingStatus;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public String getLatestOssltResult() {
        return latestOssltResult;
    }

    public String getLatestOssltDate() {
        return latestOssltDate;
    }

    public Boolean getHasOsslc() {
        return hasOsslc;
    }

    public String getOssltTrackingManualStatus() {
        return ossltTrackingManualStatus;
    }

    public String getOssltTrackingStatus() {
        return ossltTrackingStatus;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
