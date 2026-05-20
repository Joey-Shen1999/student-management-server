package com.studentmanagement.studentmanagementserver.domain.student;

import java.util.List;

public class StudentIdentityFileUploadDto {

    private Long identityFileId;
    private String identityFileName;
    private String identityFileContentType;
    private Long identityFileSizeBytes;
    private String identityFileUploadedAt;
    private Boolean hasIdentityFile;
    private List<IdentityFileItemDto> identityFiles;
    private Long version;

    public Long getIdentityFileId() {
        return identityFileId;
    }

    public void setIdentityFileId(Long identityFileId) {
        this.identityFileId = identityFileId;
    }

    public String getIdentityFileName() {
        return identityFileName;
    }

    public void setIdentityFileName(String identityFileName) {
        this.identityFileName = identityFileName;
    }

    public String getIdentityFileContentType() {
        return identityFileContentType;
    }

    public void setIdentityFileContentType(String identityFileContentType) {
        this.identityFileContentType = identityFileContentType;
    }

    public Long getIdentityFileSizeBytes() {
        return identityFileSizeBytes;
    }

    public void setIdentityFileSizeBytes(Long identityFileSizeBytes) {
        this.identityFileSizeBytes = identityFileSizeBytes;
    }

    public String getIdentityFileUploadedAt() {
        return identityFileUploadedAt;
    }

    public void setIdentityFileUploadedAt(String identityFileUploadedAt) {
        this.identityFileUploadedAt = identityFileUploadedAt;
    }

    public Boolean getHasIdentityFile() {
        return hasIdentityFile;
    }

    public void setHasIdentityFile(Boolean hasIdentityFile) {
        this.hasIdentityFile = hasIdentityFile;
    }

    public List<IdentityFileItemDto> getIdentityFiles() {
        return identityFiles;
    }

    public void setIdentityFiles(List<IdentityFileItemDto> identityFiles) {
        this.identityFiles = identityFiles;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public static class IdentityFileItemDto {
        private Long id;
        private String storageKey;
        private String identityFileName;
        private String identityFileContentType;
        private Long identityFileSizeBytes;
        private String identityFileUploadedAt;
        private Long uploadedBy;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public void setStorageKey(String storageKey) {
            this.storageKey = storageKey;
        }

        public String getIdentityFileName() {
            return identityFileName;
        }

        public void setIdentityFileName(String identityFileName) {
            this.identityFileName = identityFileName;
        }

        public String getIdentityFileContentType() {
            return identityFileContentType;
        }

        public void setIdentityFileContentType(String identityFileContentType) {
            this.identityFileContentType = identityFileContentType;
        }

        public Long getIdentityFileSizeBytes() {
            return identityFileSizeBytes;
        }

        public void setIdentityFileSizeBytes(Long identityFileSizeBytes) {
            this.identityFileSizeBytes = identityFileSizeBytes;
        }

        public String getIdentityFileUploadedAt() {
            return identityFileUploadedAt;
        }

        public void setIdentityFileUploadedAt(String identityFileUploadedAt) {
            this.identityFileUploadedAt = identityFileUploadedAt;
        }

        public Long getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(Long uploadedBy) {
            this.uploadedBy = uploadedBy;
        }
    }
}
