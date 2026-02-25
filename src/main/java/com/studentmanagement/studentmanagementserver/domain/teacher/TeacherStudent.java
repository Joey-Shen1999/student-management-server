package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

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
        name = "teacher_student",
        indexes = {
                @Index(name = "idx_teacher_student_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_teacher_student_student_id", columnList = "student_id"),
                @Index(name = "idx_teacher_student_status", columnList = "status")
        }
)
public class TeacherStudent extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStudentStatus status;

    @Column(length = 500)
    private String note;

    protected TeacherStudent() {
    }

    public TeacherStudent(Teacher teacher, Student student, TeacherStudentStatus status, String note) {
        this.teacher = teacher;
        this.student = student;
        this.status = status;
        this.note = note;
    }

    @PrePersist
    void ensureDefaults() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TeacherStudentStatus.ACTIVE;
        }
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public Student getStudent() {
        return student;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public TeacherStudentStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public void setStatus(TeacherStudentStatus status) {
        this.status = status;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
