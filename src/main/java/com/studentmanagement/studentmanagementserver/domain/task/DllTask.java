package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
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

@Entity
@Table(
        name = "dll_tasks",
        indexes = {
                @Index(name = "idx_dll_task_template", columnList = "template_id"),
                @Index(name = "idx_dll_task_assigned_student", columnList = "assigned_student_id"),
                @Index(name = "idx_dll_task_created_by_teacher", columnList = "created_by_teacher_id"),
                @Index(name = "idx_dll_task_status", columnList = "status"),
                @Index(name = "idx_dll_task_creator_updated_id", columnList = "created_by_teacher_id,updatedAt,id")
        }
)
public class DllTask extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DllTemplate template;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DllTaskStatus status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_student_id", nullable = false)
    private Student assignedStudent;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id", nullable = false)
    private Teacher createdByTeacher;

    protected DllTask() {
    }

    public DllTask(DllTemplate template,
                   String title,
                   DllTaskStatus status,
                   Student assignedStudent,
                   Teacher createdByTeacher) {
        this.template = template;
        this.title = title;
        this.status = status;
        this.assignedStudent = assignedStudent;
        this.createdByTeacher = createdByTeacher;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.status == null) {
            this.status = DllTaskStatus.NOT_STARTED;
        }
    }

    public DllTemplate getTemplate() {
        return template;
    }

    public String getTitle() {
        return title;
    }

    public DllTaskStatus getStatus() {
        return status;
    }

    public Student getAssignedStudent() {
        return assignedStudent;
    }

    public Teacher getCreatedByTeacher() {
        return createdByTeacher;
    }
}
