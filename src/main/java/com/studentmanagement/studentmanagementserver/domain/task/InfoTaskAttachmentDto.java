package com.studentmanagement.studentmanagementserver.domain.task;

public class InfoTaskAttachmentDto {

    private Long id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String uploadedAt;

    public InfoTaskAttachmentDto(Long id,
                                 String fileName,
                                 String contentType,
                                 Long sizeBytes,
                                 String uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }
}
