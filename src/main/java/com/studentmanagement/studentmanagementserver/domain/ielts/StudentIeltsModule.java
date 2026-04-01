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
    @Column(name = "language_tracking_manual_status", length = 64)
    private LanguageTrackingManualStatus languageTrackingManualStatus;

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
        this.languageTrackingManualStatus = null;
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

    public LanguageTrackingManualStatus getLanguageTrackingManualStatus() {
        return languageTrackingManualStatus;
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

    public void updateLanguageTrackingManualStatus(LanguageTrackingManualStatus languageTrackingManualStatus,
                                                   Long updatedBy,
                                                   LocalDateTime updatedAt) {
        this.languageTrackingManualStatus = languageTrackingManualStatus;
        this.languageTrackingManualStatusUpdatedBy = updatedBy;
        this.languageTrackingManualStatusUpdatedAt = updatedAt;
    }
}
