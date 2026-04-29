package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class GoalGroupStudentStatusResponseDto {
    private String taskGroupId;
    private String title;
    private String description;
    private String dueAt;
    private long totalAssigned;
    private long completedCount;
    private long pendingCount;
    private List<GoalGroupStudentStatusDto> students;

    public GoalGroupStudentStatusResponseDto(String taskGroupId,
                                             String title,
                                             String description,
                                             String dueAt,
                                             long totalAssigned,
                                             long completedCount,
                                             long pendingCount,
                                             List<GoalGroupStudentStatusDto> students) {
        this.taskGroupId = taskGroupId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.totalAssigned = totalAssigned;
        this.completedCount = completedCount;
        this.pendingCount = pendingCount;
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

    public long getTotalAssigned() {
        return totalAssigned;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public List<GoalGroupStudentStatusDto> getStudents() {
        return students;
    }
}
