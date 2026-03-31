package com.studentmanagement.studentmanagementserver.domain.student;

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
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_identity_file",
        indexes = {
                @Index(name = "idx_student_identity_file_profile_id", columnList = "student_profile_id"),
                @Index(name = "idx_student_identity_file_uploaded_at", columnList = "uploaded_at"),
                @Index(name = "idx_student_identity_profile_uploaded_id", columnList = "student_profile_id,uploaded_at,id")
        }
)
public class StudentIdentityFile extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private StudentProfile studentProfile;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    protected StudentIdentityFile() {
    }

    public StudentIdentityFile(StudentProfile studentProfile,
                               String storageKey,
                               String originalFilename,
                               String mimeType,
                               Long sizeBytes,
                               LocalDateTime uploadedAt,
                               Long uploadedBy) {
        this.studentProfile = studentProfile;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
