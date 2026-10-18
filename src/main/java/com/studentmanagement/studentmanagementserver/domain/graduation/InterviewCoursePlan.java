package com.studentmanagement.studentmanagementserver.domain.graduation;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "interview_course_plans")
public class InterviewCoursePlan extends BaseEntity {
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(nullable = false)
    private boolean enabled;

    protected InterviewCoursePlan() {}

    public InterviewCoursePlan(Student student) {
        this.student = student;
    }

    public Student getStudent() { return student; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
