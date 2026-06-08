package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(
        name = "info_task_attachments",
        indexes = {
                @Index(name = "idx_info_task_attachment_task_id", columnList = "info_task_id"),
                @Index(name = "idx_info_task_attachment_storage_key", columnList = "storage_key")
        }
)
public class InfoTaskAttachment extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "info_task_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InfoTask infoTask;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    protected InfoTaskAttachment() {
    }

    public InfoTaskAttachment(InfoTask infoTask,
                              String storageKey,
                              String originalFilename,
                              String mimeType,
                              Long sizeBytes) {
        this.infoTask = infoTask;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public InfoTask getInfoTask() {
        return infoTask;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }
}
