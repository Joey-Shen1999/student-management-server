package com.studentmanagement.studentmanagementserver.domain.task;

public class GoalTaskCompletionColumnDto {
    private String occurrenceKey;
    private String label;
    private String startAt;
    private String endAt;

    public GoalTaskCompletionColumnDto(String occurrenceKey, String label, String startAt, String endAt) {
        this.occurrenceKey = occurrenceKey;
        this.label = label;
        this.startAt = startAt;
        this.endAt = endAt;
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
}
