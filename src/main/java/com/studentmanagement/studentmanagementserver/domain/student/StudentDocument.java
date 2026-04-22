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
        name = "student_document",
        indexes = {
                @Index(name = "idx_student_document_student_id", columnList = "student_id"),
                @Index(name = "idx_student_document_uploaded_at", columnList = "uploaded_at"),
                @Index(name = "idx_student_document_student_uploaded", columnList = "student_id,uploaded_at,id"),
                @Index(name = "idx_student_document_linked_identity", columnList = "linked_identity_file_id"),
                @Index(name = "idx_student_document_linked_transcript", columnList = "linked_school_transcript_id")
        }
)
public class StudentDocument extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;

    @Column(name = "document_category", nullable = false, length = 40)
    private String documentCategory;

    @Column(name = "identity_document_type", length = 40)
    private String identityDocumentType;

    @Column(name = "academic_record_type", length = 40)
    private String academicRecordType;

    @Column(name = "report_year")
    private Integer reportYear;

    @Column(name = "report_month", length = 20)
    private String reportMonth;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "notes", length = 2000)
    private String notes;

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

    @Column(name = "linked_identity_file_id")
    private Long linkedIdentityFileId;

    @Column(name = "linked_school_record_id")
    private Long linkedSchoolRecordId;

    @Column(name = "linked_school_transcript_id")
    private Long linkedSchoolTranscriptId;

    protected StudentDocument() {
    }

    public StudentDocument(Student student,
                           String documentCategory,
                           String identityDocumentType,
                           String academicRecordType,
                           Integer reportYear,
                           String reportMonth,
                           String title,
                           String notes,
                           String storageKey,
                           String originalFilename,
                           String mimeType,
                           Long sizeBytes,
                           LocalDateTime uploadedAt,
                           Long uploadedBy) {
        this.student = student;
        this.documentCategory = documentCategory;
        this.identityDocumentType = identityDocumentType;
        this.academicRecordType = academicRecordType;
        this.reportYear = reportYear;
        this.reportMonth = reportMonth;
        this.title = title;
        this.notes = notes;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public Student getStudent() {
        return student;
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

    public Long getLinkedIdentityFileId() {
        return linkedIdentityFileId;
    }

    public void setLinkedIdentityFileId(Long linkedIdentityFileId) {
        this.linkedIdentityFileId = linkedIdentityFileId;
    }

    public Long getLinkedSchoolRecordId() {
        return linkedSchoolRecordId;
    }

    public void setLinkedSchoolRecordId(Long linkedSchoolRecordId) {
        this.linkedSchoolRecordId = linkedSchoolRecordId;
    }

    public Long getLinkedSchoolTranscriptId() {
        return linkedSchoolTranscriptId;
    }

    public void setLinkedSchoolTranscriptId(Long linkedSchoolTranscriptId) {
        this.linkedSchoolTranscriptId = linkedSchoolTranscriptId;
    }
}
