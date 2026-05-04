package com.studentmanagement.studentmanagementserver.domain.task;

public class UpdateGoalCompletionRequestDto {
    private String occurrenceKey;
    private Boolean completed;
    private String progressNote;

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public void setOccurrenceKey(String occurrenceKey) {
        this.occurrenceKey = occurrenceKey;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getProgressNote() {
        return progressNote;
    }

    public void setProgressNote(String progressNote) {
        this.progressNote = progressNote;
    }
}
