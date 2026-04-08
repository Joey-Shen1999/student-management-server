package com.studentmanagement.studentmanagementserver.domain.volunteer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VolunteerTrackingTaskUpsertDto {
    private String taskName;
    private String description;
    private BigDecimal durationHours;
    private LocalDate startDate;
    private LocalDate endDate;
    private String verifierContact;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours) {
        this.durationHours = durationHours;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getVerifierContact() {
        return verifierContact;
    }

    public void setVerifierContact(String verifierContact) {
        this.verifierContact = verifierContact;
    }
}
