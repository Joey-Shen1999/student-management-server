package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class GoalGroupStudentStatusResponseDto {
    private String taskGroupId;
    private String title;
    private String description;
    private String dueAt;
    private String cycleType;
    private String cycleFrequency;
    private Integer cycleInterval;
    private String cycleUnit;
    private String cycleLabel;
    private String cycleEndAt;
    private boolean cycleNoEnd;
    private long totalAssigned;
    private long completedCount;
    private long pendingCount;
    private List<GoalTaskCompletionColumnDto> completionColumns;
    private List<GoalGroupStudentStatusDto> students;

    public GoalGroupStudentStatusResponseDto(String taskGroupId,
                                             String title,
                                             String description,
                                             String dueAt,
                                             String cycleType,
                                             String cycleFrequency,
                                             Integer cycleInterval,
                                             String cycleUnit,
                                             String cycleLabel,
                                             String cycleEndAt,
                                             boolean cycleNoEnd,
                                             long totalAssigned,
                                             long completedCount,
                                             long pendingCount,
                                             List<GoalTaskCompletionColumnDto> completionColumns,
                                             List<GoalGroupStudentStatusDto> students) {
        this.taskGroupId = taskGroupId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.cycleType = cycleType;
        this.cycleFrequency = cycleFrequency;
        this.cycleInterval = cycleInterval;
        this.cycleUnit = cycleUnit;
        this.cycleLabel = cycleLabel;
        this.cycleEndAt = cycleEndAt;
        this.cycleNoEnd = cycleNoEnd;
        this.totalAssigned = totalAssigned;
        this.completedCount = completedCount;
        this.pendingCount = pendingCount;
        this.completionColumns = completionColumns;
        this.students = students;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDueAt() {
        return dueAt;
    }

    public String getCycleType() {
        return cycleType;
    }

    public String getCycleFrequency() {
        return cycleFrequency;
    }

    public Integer getCycleInterval() {
        return cycleInterval;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public String getCycleLabel() {
        return cycleLabel;
    }

    public String getCycleEndAt() {
        return cycleEndAt;
    }

    public boolean isCycleNoEnd() {
        return cycleNoEnd;
    }

    public long getTotalAssigned() {
        return totalAssigned;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public List<GoalTaskCompletionColumnDto> getCompletionColumns() {
        return completionColumns;
    }

    public List<GoalGroupStudentStatusDto> getStudents() {
        return students;
    }
}
