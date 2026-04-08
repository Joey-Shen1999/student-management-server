package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class CreateInfoRequestDto {
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private List<Long> studentIds;
    private String taskGroupId;
    private Long goalId;
    private CreateInfoVolunteerDto volunteer;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public void setTaskGroupId(String taskGroupId) {
        this.taskGroupId = taskGroupId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public CreateInfoVolunteerDto getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(CreateInfoVolunteerDto volunteer) {
        this.volunteer = volunteer;
    }
}
