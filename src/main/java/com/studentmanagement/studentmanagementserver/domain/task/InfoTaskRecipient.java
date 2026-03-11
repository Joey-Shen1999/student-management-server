package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "info_task_recipients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_info_task_recipient_task_student", columnNames = {"info_task_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_info_recipient_student", columnList = "student_id"),
                @Index(name = "idx_info_recipient_task", columnList = "info_task_id"),
                @Index(name = "idx_info_recipient_read", columnList = "is_read")
        }
)
public class InfoTaskRecipient extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "info_task_id", nullable = false)
    private InfoTask infoTask;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected InfoTaskRecipient() {
    }

    public InfoTaskRecipient(InfoTask infoTask, Student student) {
        this.infoTask = infoTask;
        this.student = student;
        this.read = false;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.readAt == null && this.read) {
            this.readAt = LocalDateTime.now();
        }
    }

    public void markRead() {
        this.read = true;
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public InfoTask getInfoTask() {
        return infoTask;
    }

    public Student getStudent() {
        return student;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
