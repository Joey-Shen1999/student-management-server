package com.studentmanagement.studentmanagementserver.domain.task;

public class GoalTaskCompletionDto {
    private Long id;
    private String occurrenceKey;
    private String label;
    private String startAt;
    private String endAt;
    private boolean completed;
    private String completedAt;
    private String updatedAt;
    private String progressNote;

    public GoalTaskCompletionDto(Long id,
                                 String occurrenceKey,
                                 String label,
                                 String startAt,
                                 String endAt,
                                 boolean completed,
                                 String completedAt,
                                 String updatedAt,
                                 String progressNote) {
        this.id = id;
        this.occurrenceKey = occurrenceKey;
        this.label = label;
        this.startAt = startAt;
        this.endAt = endAt;
        this.completed = completed;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
        this.progressNote = progressNote;
    }

    public Long getId() {
        return id;
    }

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public String getLabel() {
        return label;
    }

    public String getStartAt() {
        return startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getProgressNote() {
        return progressNote;
    }
}
