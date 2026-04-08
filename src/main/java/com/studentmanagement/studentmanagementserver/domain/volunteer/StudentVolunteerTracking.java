package com.studentmanagement.studentmanagementserver.domain.volunteer;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
        name = "student_volunteer_tracking",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_volunteer_tracking_student_id", columnNames = {"student_id"})
        },
        indexes = {
                @Index(name = "idx_student_volunteer_tracking_student_id", columnList = "student_id"),
                @Index(name = "idx_student_volunteer_tracking_updated_by_teacher_id", columnList = "updated_by_teacher_id")
        }
)
public class StudentVolunteerTracking extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "total_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "note", length = 2000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_teacher_id")
    private Teacher updatedByTeacher;

    protected StudentVolunteerTracking() {
    }

    public StudentVolunteerTracking(Student student,
                                    BigDecimal totalHours,
                                    String note,
                                    Teacher updatedByTeacher) {
        this.student = student;
        this.totalHours = totalHours;
        this.note = note;
        this.updatedByTeacher = updatedByTeacher;
    }

    public void overwrite(BigDecimal totalHours, String note, Teacher updatedByTeacher) {
        this.totalHours = totalHours;
        this.note = note;
        this.updatedByTeacher = updatedByTeacher;
    }

    public Student getStudent() {
        return student;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public String getNote() {
        return note;
    }

    public Teacher getUpdatedByTeacher() {
        return updatedByTeacher;
    }
}
