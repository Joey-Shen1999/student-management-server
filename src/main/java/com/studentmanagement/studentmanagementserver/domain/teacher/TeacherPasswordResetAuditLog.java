package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.user.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "teacher_password_reset_audit_logs",
        indexes = {
                @Index(name = "idx_teacher_reset_audit_teacher_id", columnList = "teacherId"),
                @Index(name = "idx_teacher_reset_audit_operator_user_id", columnList = "operatorUserId")
        }
)
public class TeacherPasswordResetAuditLog extends BaseEntity {

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 80)
    private String targetUsername;

    @Column(nullable = false)
    private Long operatorUserId;

    @Column(nullable = false, length = 80)
    private String operatorUsername;

    @Column(nullable = false)
    private LocalDateTime resetAt;

    protected TeacherPasswordResetAuditLog() {
    }

    public TeacherPasswordResetAuditLog(User operator, Teacher teacher) {
        this.teacherId = teacher.getId();
        this.targetUserId = teacher.getUser().getId();
        this.targetUsername = teacher.getUser().getUsername();
        this.operatorUserId = operator.getId();
        this.operatorUsername = operator.getUsername();
        this.resetAt = LocalDateTime.now();
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }
}
