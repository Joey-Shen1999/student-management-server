package com.studentmanagement.studentmanagementserver.domain.osslt;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_osslt_module",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_osslt_module_student_id", columnNames = "student_id")
        },
        indexes = {
                @Index(name = "idx_student_osslt_module_student_id", columnList = "student_id"),
                @Index(name = "idx_student_osslt_module_tracking_status", columnList = "osslt_tracking_status")
        }
)
public class StudentOssltModule extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_osslt_result", nullable = false, length = 16)
    private OssltLatestResult latestOssltResult;

    @Column(name = "latest_osslt_date")
    private LocalDate latestOssltDate;

    @Column(name = "has_osslc")
    private Boolean hasOsslc;

    @Enumerated(EnumType.STRING)
    @Column(name = "osslt_tracking_manual_status", length = 32)
    private OssltTrackingManualStatus ossltTrackingManualStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "osslt_tracking_status", nullable = false, length = 32)
    private OssltTrackingStatus ossltTrackingStatus;

    @Column(name = "osslt_updated_at", nullable = false)
    private LocalDateTime ossltUpdatedAt;

    protected StudentOssltModule() {
    }

    public StudentOssltModule(Student student) {
        this.student = student;
        this.latestOssltResult = OssltLatestResult.UNKNOWN;
        this.latestOssltDate = null;
        this.hasOsslc = null;
        this.ossltTrackingManualStatus = null;
        this.ossltTrackingStatus = OssltTrackingStatus.WAITING_UPDATE;
        this.ossltUpdatedAt = LocalDateTime.now();
    }

    public Student getStudent() {
        return student;
    }

    public OssltLatestResult getLatestOssltResult() {
        return latestOssltResult;
    }

    public LocalDate getLatestOssltDate() {
        return latestOssltDate;
    }

    public Boolean getHasOsslc() {
        return hasOsslc;
    }

    public OssltTrackingManualStatus getOssltTrackingManualStatus() {
        return ossltTrackingManualStatus;
    }

    public OssltTrackingStatus getOssltTrackingStatus() {
        return ossltTrackingStatus;
    }

    public LocalDateTime getOssltUpdatedAt() {
        return ossltUpdatedAt;
    }

    public void updateLatestOssltResult(OssltLatestResult latestOssltResult) {
        this.latestOssltResult = latestOssltResult == null ? OssltLatestResult.UNKNOWN : latestOssltResult;
    }

    public void updateLatestOssltDate(LocalDate latestOssltDate) {
        this.latestOssltDate = latestOssltDate;
    }

    public void updateHasOsslc(Boolean hasOsslc) {
        this.hasOsslc = hasOsslc;
    }

    public void updateOssltTrackingManualStatus(OssltTrackingManualStatus ossltTrackingManualStatus) {
        this.ossltTrackingManualStatus = ossltTrackingManualStatus;
    }

    public void updateOssltTrackingStatus(OssltTrackingStatus ossltTrackingStatus) {
        this.ossltTrackingStatus = ossltTrackingStatus == null
                ? OssltTrackingStatus.WAITING_UPDATE
                : ossltTrackingStatus;
    }

    public void markOssltUpdatedAt(LocalDateTime ossltUpdatedAt) {
        this.ossltUpdatedAt = ossltUpdatedAt == null ? LocalDateTime.now() : ossltUpdatedAt;
    }
}
