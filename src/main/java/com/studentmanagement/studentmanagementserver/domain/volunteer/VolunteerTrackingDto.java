package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.math.BigDecimal;
import java.util.List;

public class VolunteerTrackingDto {
    private Long studentId;
    private BigDecimal totalHours;
    private String note;
    private List<VolunteerTrackingTaskDto> tasks;
    private String updatedAt;
    private Long updatedByTeacherId;

    public VolunteerTrackingDto(Long studentId,
                                BigDecimal totalHours,
                                String note,
                                List<VolunteerTrackingTaskDto> tasks,
                                String updatedAt,
                                Long updatedByTeacherId) {
        this.studentId = studentId;
        this.totalHours = totalHours;
        this.note = note;
        this.tasks = tasks;
        this.updatedAt = updatedAt;
        this.updatedByTeacherId = updatedByTeacherId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public String getNote() {
        return note;
    }

    public List<VolunteerTrackingTaskDto> getTasks() {
        return tasks;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedByTeacherId() {
        return updatedByTeacherId;
    }
}
