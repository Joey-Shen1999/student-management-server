package com.studentmanagement.studentmanagementserver.domain.student;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.StudentInviteStatus;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_invites",
        indexes = {
                @Index(name = "idx_student_invites_token", columnList = "invite_token", unique = true),
                @Index(name = "idx_student_invites_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_student_invites_status", columnList = "status")
        }
)
public class StudentInvite extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "invite_token", nullable = false, unique = true, length = 120)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentInviteStatus status = StudentInviteStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private Long usedUserId;

    protected StudentInvite() {
    }

    public StudentInvite(Teacher teacher, String inviteToken, LocalDateTime expiresAt) {
        this.teacher = teacher;
        this.inviteToken = inviteToken;
        this.expiresAt = expiresAt;
        this.status = StudentInviteStatus.PENDING;
    }

    @PrePersist
    void ensureDefaults() {
        if (status == null) {
            status = StudentInviteStatus.PENDING;
        }
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public StudentInviteStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public Long getUsedUserId() {
        return usedUserId;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void markUsed(Long userId) {
        this.status = StudentInviteStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.usedUserId = userId;
    }

    public void markExpired() {
        this.status = StudentInviteStatus.EXPIRED;
    }

    public void markRevoked() {
        this.status = StudentInviteStatus.REVOKED;
    }
}
