package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class CreateInfoVolunteerDto {
    private List<CreateInfoVolunteerTaskItemDto> tasks;

    public List<CreateInfoVolunteerTaskItemDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<CreateInfoVolunteerTaskItemDto> tasks) {
        this.tasks = tasks;
    }
}
