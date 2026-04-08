package com.studentmanagement.studentmanagementserver.domain.task;

import java.math.BigDecimal;
import java.util.List;

public class InfoTaskVolunteerDto {
    private BigDecimal totalHours;
    private List<InfoTaskVolunteerTaskItemDto> tasks;

    public InfoTaskVolunteerDto(BigDecimal totalHours, List<InfoTaskVolunteerTaskItemDto> tasks) {
        this.totalHours = totalHours;
        this.tasks = tasks;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public List<InfoTaskVolunteerTaskItemDto> getTasks() {
        return tasks;
    }
}
