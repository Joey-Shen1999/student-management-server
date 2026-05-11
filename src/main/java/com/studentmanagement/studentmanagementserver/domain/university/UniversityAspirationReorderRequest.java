package com.studentmanagement.studentmanagementserver.domain.university;

public class UniversityAspirationReorderRequest {
    private Long id;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAspirationId() {
        return id;
    }

    public void setAspirationId(Long aspirationId) {
        this.id = aspirationId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
