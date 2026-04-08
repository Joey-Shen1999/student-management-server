package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.math.BigDecimal;

public class VolunteerTrackingBatchSummaryItemDto {
    private Long studentId;
    private BigDecimal totalVolunteerHours;
    private boolean volunteerCompleted;
    private String updatedAt;

    public VolunteerTrackingBatchSummaryItemDto(Long studentId,
                                                BigDecimal totalVolunteerHours,
                                                boolean volunteerCompleted,
                                                String updatedAt) {
        this.studentId = studentId;
        this.totalVolunteerHours = totalVolunteerHours;
        this.volunteerCompleted = volunteerCompleted;
        this.updatedAt = updatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BigDecimal getTotalVolunteerHours() {
        return totalVolunteerHours;
    }

    public boolean isVolunteerCompleted() {
        return volunteerCompleted;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
