package com.studentmanagement.studentmanagementserver.domain.courseplan;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "student_course_plan_course",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_course_plan_course_key", columnNames = {"grade_id", "client_course_id"})
        })
public class StudentCoursePlanCourse extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private StudentCoursePlanGrade grade;

    @Column(name = "client_course_id", nullable = false, length = 128)
    private String clientCourseId;

    @Column(name = "course_code", length = 64)
    private String courseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CoursePlanCourseStatus status;

    @Column(name = "mark")
    private Integer mark;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester", length = 2)
    private CoursePlanSemester semester;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected StudentCoursePlanCourse() {
    }

    public StudentCoursePlanCourse(String clientCourseId,
                                   String courseCode,
                                   CoursePlanCourseStatus status,
                                   Integer mark,
                                   CoursePlanSemester semester,
                                   int sortOrder) {
        this.clientCourseId = clientCourseId;
        this.courseCode = courseCode;
        this.status = status;
        this.mark = mark;
        this.semester = semester;
        this.sortOrder = sortOrder;
    }

    public StudentCoursePlanGrade getGrade() {
        return grade;
    }

    public String getClientCourseId() {
        return clientCourseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public CoursePlanCourseStatus getStatus() {
        return status;
    }

    public Integer getMark() {
        return mark;
    }

    public CoursePlanSemester getSemester() {
        return semester;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    void attachToGrade(StudentCoursePlanGrade grade) {
        this.grade = grade;
    }
}
