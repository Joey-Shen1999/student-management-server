package com.studentmanagement.studentmanagementserver.domain.ielts;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_ielts_manual_status_audit_log",
        indexes = {
                @Index(name = "idx_ielts_manual_audit_student_id", columnList = "student_id"),
                @Index(name = "idx_ielts_manual_audit_operator_user_id", columnList = "operator_user_id"),
                @Index(name = "idx_ielts_manual_audit_changed_at", columnList = "changed_at")
        }
)
public class StudentIeltsManualStatusAuditLog extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Column(name = "operator_role", nullable = false, length = 20)
    private String operatorRole;

    @Column(name = "previous_manual_status", length = 64)
    private String previousManualStatus;

    @Column(name = "current_manual_status", length = 64)
    private String currentManualStatus;

    @Column(name = "change_source", nullable = false, length = 40)
    private String changeSource;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected StudentIeltsManualStatusAuditLog() {
    }

    public StudentIeltsManualStatusAuditLog(Long studentId,
                                            User operator,
                                            String previousManualStatus,
                                            String currentManualStatus,
                                            String changeSource,
                                            LocalDateTime changedAt) {
        if (operator == null || operator.getId() == null) {
            throw new IllegalArgumentException("operator is required for manual status audit");
        }
        this.studentId = studentId;
        this.operatorUserId = operator.getId();
        UserRole role = operator.getRole();
        this.operatorRole = role == null ? "UNKNOWN" : role.name();
        this.previousManualStatus = previousManualStatus;
        this.currentManualStatus = currentManualStatus;
        this.changeSource = changeSource;
        this.changedAt = changedAt == null ? LocalDateTime.now() : changedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public String getPreviousManualStatus() {
        return previousManualStatus;
    }

    public String getCurrentManualStatus() {
        return currentManualStatus;
    }

    public String getChangeSource() {
        return changeSource;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
