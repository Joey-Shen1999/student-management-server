package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.math.BigDecimal;
import java.util.List;

public class VolunteerTrackingUpsertRequestDto {
    private BigDecimal totalHours;
    private String note;
    private List<VolunteerTrackingTaskUpsertDto> tasks;

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<VolunteerTrackingTaskUpsertDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<VolunteerTrackingTaskUpsertDto> tasks) {
        this.tasks = tasks;
    }
}
