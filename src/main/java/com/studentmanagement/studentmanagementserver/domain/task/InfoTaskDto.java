package com.studentmanagement.studentmanagementserver.domain.task;

import java.util.List;

public class InfoTaskDto {
    private Long id;
    private String type;
    private String title;
    private String content;
    private InfoTaskCategory category;
    private List<String> tags;
    private Long goalId;
    private String taskGroupId;
    private List<Long> recipientStudentIds;
    private int targetStudentCount;
    private Long publishedByTeacherId;
    private String publishedByTeacherName;
    private String createdAt;
    private String updatedAt;
    private boolean read;
    private String readAt;

    public InfoTaskDto(Long id,
                       String type,
                       String title,
                       String content,
                       InfoTaskCategory category,
                       List<String> tags,
                       Long goalId,
                       String taskGroupId,
                       List<Long> recipientStudentIds,
                       int targetStudentCount,
                       Long publishedByTeacherId,
                       String publishedByTeacherName,
                       String createdAt,
                       String updatedAt,
                       boolean read,
                       String readAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.category = category;
        this.tags = tags;
        this.goalId = goalId;
        this.taskGroupId = taskGroupId;
        this.recipientStudentIds = recipientStudentIds;
        this.targetStudentCount = targetStudentCount;
        this.publishedByTeacherId = publishedByTeacherId;
        this.publishedByTeacherName = publishedByTeacherName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.read = read;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public InfoTaskCategory getCategory() {
        return category;
    }

    public List<String> getTags() {
        return tags;
    }

    public Long getGoalId() {
        return goalId;
    }

    public String getTaskGroupId() {
        return taskGroupId;
    }

    public List<Long> getRecipientStudentIds() {
        return recipientStudentIds;
    }

    public int getTargetStudentCount() {
        return targetStudentCount;
    }

    public Long getPublishedByTeacherId() {
        return publishedByTeacherId;
    }

    public String getPublishedByTeacherName() {
        return publishedByTeacherName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isRead() {
        return read;
    }

    public String getReadAt() {
        return readAt;
    }
}
