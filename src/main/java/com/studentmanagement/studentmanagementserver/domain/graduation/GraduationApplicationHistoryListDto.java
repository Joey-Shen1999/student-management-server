package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

public class GraduationApplicationHistoryListDto {

    private List<ItemDto> items = new ArrayList<ItemDto>();
    private long total;
    private int page;
    private int size;

    public List<ItemDto> getItems() {
        return items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items == null ? new ArrayList<ItemDto>() : items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public static class ItemDto {
        private Long id;
        private Long studentId;
        private Long applicationId;
        private String operation;
        private Long actorUserId;
        private String actorRole;
        private String actorName;
        private String changedAt;
        private List<FieldChangeDto> changedFields = new ArrayList<FieldChangeDto>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public Long getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(Long applicationId) {
            this.applicationId = applicationId;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Long getActorUserId() {
            return actorUserId;
        }

        public void setActorUserId(Long actorUserId) {
            this.actorUserId = actorUserId;
        }

        public String getActorRole() {
            return actorRole;
        }

        public void setActorRole(String actorRole) {
            this.actorRole = actorRole;
        }

        public String getActorName() {
            return actorName;
        }

        public void setActorName(String actorName) {
            this.actorName = actorName;
        }

        public String getChangedAt() {
            return changedAt;
        }

        public void setChangedAt(String changedAt) {
            this.changedAt = changedAt;
        }

        public List<FieldChangeDto> getChangedFields() {
            return changedFields;
        }

        public void setChangedFields(List<FieldChangeDto> changedFields) {
            this.changedFields = changedFields == null ? new ArrayList<FieldChangeDto>() : changedFields;
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class FieldChangeDto {
        private String path;
        private String label;
        private Object before;
        private Object after;

        public FieldChangeDto() {
        }

        public FieldChangeDto(String path, String label, Object before, Object after) {
            this.path = path;
            this.label = label;
            this.before = before;
            this.after = after;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Object getBefore() {
            return before;
        }

        public void setBefore(Object before) {
            this.before = before;
        }

        public Object getAfter() {
            return after;
        }

        public void setAfter(Object after) {
            this.after = after;
        }
    }
}
