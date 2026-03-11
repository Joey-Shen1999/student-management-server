package com.studentmanagement.studentmanagementserver.domain.task;

public class DllTemplateDto {
    private Long id;
    private String name;
    private String description;
    private String payloadSchema;
    private String createdAt;
    private String updatedAt;

    public DllTemplateDto(Long id,
                          String name,
                          String description,
                          String payloadSchema,
                          String createdAt,
                          String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.payloadSchema = payloadSchema;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPayloadSchema() {
        return payloadSchema;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
