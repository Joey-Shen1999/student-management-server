package com.studentmanagement.studentmanagementserver.domain.task;

public class UpdateGoalStatusRequestDto {
    private String status;
    private String progressNote;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProgressNote() {
        return progressNote;
    }

    public void setProgressNote(String progressNote) {
        this.progressNote = progressNote;
    }
}
