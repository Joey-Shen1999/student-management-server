package com.studentmanagement.studentmanagementserver.domain.ielts;

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
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_ielts_module",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_ielts_module_student_id", columnNames = "student_id")
        },
        indexes = {
                @Index(name = "idx_student_ielts_module_student_id", columnList = "student_id")
        }
)
public class StudentIeltsModule extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "has_taken_ielts_academic", nullable = false)
    private boolean hasTakenIeltsAcademic;

    @Enumerated(EnumType.STRING)
    @Column(name = "preparation_intent", nullable = false, length = 20)
    private IeltsPreparationIntent preparationIntent;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_score_type", nullable = false, length = 20)
    private LanguageScoreType languageScoreType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_tracking_manual_status", length = 64)
    private LanguageTrackingManualStatus languageTrackingManualStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_score_tracking_manual_status", length = 64)
    private LanguageTrackingManualStatus languageScoreTrackingManualStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_status", nullable = false, length = 64)
    private IeltsTrackingStatus trackingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_tracking_status", nullable = false, length = 64)
    private LanguageTrackingStatus languageTrackingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_score_tracking_status", nullable = false, length = 64)
    private LanguageTrackingStatus languageScoreTrackingStatus;

    @Column(name = "language_tracking_manual_status_updated_by")
    private Long languageTrackingManualStatusUpdatedBy;

    @Column(name = "language_tracking_manual_status_updated_at")
    private LocalDateTime languageTrackingManualStatusUpdatedAt;

    protected StudentIeltsModule() {
    }

    public StudentIeltsModule(Student student) {
        this.student = student;
        this.hasTakenIeltsAcademic = false;
        this.preparationIntent = IeltsPreparationIntent.UNSET;
        this.languageScoreType = LanguageScoreType.IELTS;
        this.languageTrackingManualStatus = null;
        this.languageScoreTrackingManualStatus = null;
        this.trackingStatus = IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION;
        this.languageTrackingStatus = LanguageTrackingStatus.NEEDS_TRACKING;
        this.languageScoreTrackingStatus = LanguageTrackingStatus.NEEDS_TRACKING;
    }

    public Student getStudent() {
        return student;
    }

    public boolean isHasTakenIeltsAcademic() {
        return hasTakenIeltsAcademic;
    }

    public IeltsPreparationIntent getPreparationIntent() {
        return preparationIntent;
    }

    public LanguageScoreType getLanguageScoreType() {
        return languageScoreType;
    }

    public LanguageTrackingManualStatus getLanguageTrackingManualStatus() {
        if (languageScoreTrackingManualStatus != null) {
            return languageScoreTrackingManualStatus;
        }
        return languageTrackingManualStatus;
    }

    public LanguageTrackingManualStatus getLanguageScoreTrackingManualStatus() {
        return getLanguageTrackingManualStatus();
    }

    public IeltsTrackingStatus getTrackingStatus() {
        return trackingStatus;
    }

    public LanguageTrackingStatus getLanguageTrackingStatus() {
        if (languageScoreTrackingStatus != null) {
            return languageScoreTrackingStatus;
        }
        return languageTrackingStatus;
    }

    public LanguageTrackingStatus getLanguageScoreTrackingStatus() {
        return getLanguageTrackingStatus();
    }

    public Long getLanguageTrackingManualStatusUpdatedBy() {
        return languageTrackingManualStatusUpdatedBy;
    }

    public LocalDateTime getLanguageTrackingManualStatusUpdatedAt() {
        return languageTrackingManualStatusUpdatedAt;
    }

    public void updateState(boolean hasTakenIeltsAcademic, IeltsPreparationIntent preparationIntent) {
        this.hasTakenIeltsAcademic = hasTakenIeltsAcademic;
        this.preparationIntent = preparationIntent == null ? IeltsPreparationIntent.UNSET : preparationIntent;
    }

    public void updateLanguageScoreType(LanguageScoreType languageScoreType) {
        this.languageScoreType = languageScoreType == null ? LanguageScoreType.IELTS : languageScoreType;
    }

    public void updateLanguageTrackingManualStatus(LanguageTrackingManualStatus languageTrackingManualStatus,
                                                   Long updatedBy,
                                                   LocalDateTime updatedAt) {
        this.languageTrackingManualStatus = languageTrackingManualStatus;
        this.languageScoreTrackingManualStatus = languageTrackingManualStatus;
        this.languageTrackingManualStatusUpdatedBy = updatedBy;
        this.languageTrackingManualStatusUpdatedAt = updatedAt;
    }

    public void updateDerivedStatuses(IeltsTrackingStatus trackingStatus,
                                      LanguageTrackingStatus languageTrackingStatus) {
        this.trackingStatus = trackingStatus == null
                ? IeltsTrackingStatus.YELLOW_NEEDS_PREPARATION
                : trackingStatus;
        this.languageTrackingStatus = languageTrackingStatus == null
                ? LanguageTrackingStatus.NEEDS_TRACKING
                : languageTrackingStatus;
        this.languageScoreTrackingStatus = this.languageTrackingStatus;
    }

    public void syncLanguageTrackingCompatibilityFields() {
        if (languageScoreTrackingManualStatus == null && languageTrackingManualStatus != null) {
            languageScoreTrackingManualStatus = languageTrackingManualStatus;
        } else if (languageTrackingManualStatus == null && languageScoreTrackingManualStatus != null) {
            languageTrackingManualStatus = languageScoreTrackingManualStatus;
        }

        if (languageScoreTrackingStatus == null && languageTrackingStatus != null) {
            languageScoreTrackingStatus = languageTrackingStatus;
        } else if (languageTrackingStatus == null && languageScoreTrackingStatus != null) {
            languageTrackingStatus = languageScoreTrackingStatus;
        }

        if (languageScoreTrackingStatus == null) {
            languageScoreTrackingStatus = LanguageTrackingStatus.NEEDS_TRACKING;
        }
        if (languageTrackingStatus == null) {
            languageTrackingStatus = languageScoreTrackingStatus;
        }
    }
}
