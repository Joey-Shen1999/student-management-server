package com.studentmanagement.studentmanagementserver.domain.task;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InfoTaskVolunteerTaskItemDto {
    private String taskName;
    private String description;
    private BigDecimal durationHours;
    private LocalDate startDate;
    private LocalDate endDate;
    private String verifierContact;

    public InfoTaskVolunteerTaskItemDto(String taskName,
                                        String description,
                                        BigDecimal durationHours,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        String verifierContact) {
        this.taskName = taskName;
        this.description = description;
        this.durationHours = durationHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.verifierContact = verifierContact;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getVerifierContact() {
        return verifierContact;
    }
}
