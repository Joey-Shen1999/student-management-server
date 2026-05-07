package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

import java.time.LocalDateTime;

public class ServiceProgressRecordRequestDto {
    private LocalDateTime appointmentTime;
    private Long advisorId;
    private String followUpContent;
    private String nextPlan;

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public Long getAdvisorId() {
        return advisorId;
    }

    public void setAdvisorId(Long advisorId) {
        this.advisorId = advisorId;
    }

    public String getFollowUpContent() {
        return followUpContent;
    }

    public void setFollowUpContent(String followUpContent) {
        this.followUpContent = followUpContent;
    }

    public String getNextPlan() {
        return nextPlan;
    }

    public void setNextPlan(String nextPlan) {
        this.nextPlan = nextPlan;
    }
}
