package com.studentmanagement.studentmanagementserver.domain.graduation;

public class GraduationApplicationRequest {
    private Long universityId;
    private Long programId;
    private GraduationApplicationStatus status;
    private Long sourceAspirationId;

    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public GraduationApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(GraduationApplicationStatus status) {
        this.status = status;
    }

    public Long getSourceAspirationId() {
        return sourceAspirationId;
    }

    public void setSourceAspirationId(Long sourceAspirationId) {
        this.sourceAspirationId = sourceAspirationId;
    }
}
