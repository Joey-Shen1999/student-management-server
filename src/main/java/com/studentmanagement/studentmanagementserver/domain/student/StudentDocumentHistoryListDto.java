package com.studentmanagement.studentmanagementserver.domain.student;

import java.util.ArrayList;
import java.util.List;

public class StudentDocumentHistoryListDto {

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
        private Long documentId;
        private String action;
        private String documentCategory;
        private String identityDocumentType;
        private String academicRecordType;
        private Integer reportYear;
        private String reportMonth;
        private String title;
        private String notes;
        private String fileName;
        private String contentType;
        private Long sizeBytes;
        private Long actorUserId;
        private String actorRole;
        private String actorName;
        private String actionAt;

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

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getDocumentCategory() {
            return documentCategory;
        }

        public void setDocumentCategory(String documentCategory) {
            this.documentCategory = documentCategory;
        }

        public String getIdentityDocumentType() {
            return identityDocumentType;
        }

        public void setIdentityDocumentType(String identityDocumentType) {
            this.identityDocumentType = identityDocumentType;
        }

        public String getAcademicRecordType() {
            return academicRecordType;
        }

        public void setAcademicRecordType(String academicRecordType) {
            this.academicRecordType = academicRecordType;
        }

        public Integer getReportYear() {
            return reportYear;
        }

        public void setReportYear(Integer reportYear) {
            this.reportYear = reportYear;
        }

        public String getReportMonth() {
            return reportMonth;
        }

        public void setReportMonth(String reportMonth) {
            this.reportMonth = reportMonth;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
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

        public String getActionAt() {
            return actionAt;
        }

        public void setActionAt(String actionAt) {
            this.actionAt = actionAt;
        }
    }
}
