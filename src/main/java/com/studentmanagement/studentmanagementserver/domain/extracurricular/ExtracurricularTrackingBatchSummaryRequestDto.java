package com.studentmanagement.studentmanagementserver.domain.extracurricular;

import java.util.List;

public class ExtracurricularTrackingBatchSummaryRequestDto {
    private List<Long> studentIds;

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }
}
