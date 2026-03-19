package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_school_record",
        indexes = @Index(name = "idx_student_school_record_student_id", columnList = "student_id")
)
public class StudentSchoolRecord extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "school_type", nullable = false, length = 10)
    private SchoolType schoolType;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    @Column(name = "school_board", length = 64)
    private String schoolBoard;

    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "postal", length = 30)
    private String postal;

    @Column(name = "start_time")
    private LocalDate startTime;

    @Column(name = "end_time")
    private LocalDate endTime;

    @Column(name = "transcript_original_filename", length = 255)
    private String transcriptOriginalFilename;

    @Column(name = "transcript_content_type", length = 120)
    private String transcriptContentType;

    @Column(name = "transcript_storage_key", length = 255)
    private String transcriptStorageKey;

    @Column(name = "transcript_size_bytes")
    private Long transcriptSizeBytes;

    @Column(name = "transcript_uploaded_at")
    private LocalDateTime transcriptUploadedAt;

    protected StudentSchoolRecord() {
    }

    public StudentSchoolRecord(Student student,
                               SchoolType schoolType,
                               String schoolName,
                               LocalDate startTime,
                               LocalDate endTime) {
        this(student, schoolType, schoolName, null, null, null, null, null, null, startTime, endTime);
    }

    public StudentSchoolRecord(Student student,
                               SchoolType schoolType,
                               String schoolName,
                               String schoolBoard,
                               String streetAddress,
                               String city,
                               String state,
                               String country,
                               String postal,
                               LocalDate startTime,
                               LocalDate endTime) {
        this.student = student;
        this.schoolType = schoolType;
        this.schoolName = schoolName;
        this.schoolBoard = schoolBoard;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postal = postal;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Student getStudent() {
        return student;
    }

    public SchoolType getSchoolType() {
        return schoolType;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public String getSchoolBoard() {
        return schoolBoard;
    }

    public void setSchoolBoard(String schoolBoard) {
        this.schoolBoard = schoolBoard;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPostal() {
        return postal;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public LocalDate getEndTime() {
        return endTime;
    }

    public String getTranscriptOriginalFilename() {
        return transcriptOriginalFilename;
    }

    public void setTranscriptOriginalFilename(String transcriptOriginalFilename) {
        this.transcriptOriginalFilename = transcriptOriginalFilename;
    }

    public String getTranscriptContentType() {
        return transcriptContentType;
    }

    public void setTranscriptContentType(String transcriptContentType) {
        this.transcriptContentType = transcriptContentType;
    }

    public String getTranscriptStorageKey() {
        return transcriptStorageKey;
    }

    public void setTranscriptStorageKey(String transcriptStorageKey) {
        this.transcriptStorageKey = transcriptStorageKey;
    }

    public Long getTranscriptSizeBytes() {
        return transcriptSizeBytes;
    }

    public void setTranscriptSizeBytes(Long transcriptSizeBytes) {
        this.transcriptSizeBytes = transcriptSizeBytes;
    }

    public LocalDateTime getTranscriptUploadedAt() {
        return transcriptUploadedAt;
    }

    public void setTranscriptUploadedAt(LocalDateTime transcriptUploadedAt) {
        this.transcriptUploadedAt = transcriptUploadedAt;
    }
}
