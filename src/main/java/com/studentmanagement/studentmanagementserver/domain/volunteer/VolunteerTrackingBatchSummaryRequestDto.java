package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.util.List;

public class VolunteerTrackingBatchSummaryRequestDto {
    private List<Long> studentIds;

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }
}
