package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class GoalGroupResponseDto {
    private String taskGroupId;
    private List<GoalTaskDto> items;
    private long total;

    public GoalGroupResponseDto(String taskGroupId, List<GoalTaskDto> items, long total) {
        this.taskGroupId = taskGroupId;
        this.items = items;
        this.total = total;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public List<GoalTaskDto> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }
}
