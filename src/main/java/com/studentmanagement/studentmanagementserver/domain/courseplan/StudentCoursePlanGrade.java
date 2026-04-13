package com.studentmanagement.studentmanagementserver.domain.courseplan;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_course_plan_grade",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_course_plan_grade_level", columnNames = {"course_plan_id", "grade_level"})
        })
public class StudentCoursePlanGrade extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_plan_id", nullable = false)
    private StudentCoursePlan coursePlan;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "year_structure", nullable = false, length = 16)
    private CoursePlanYearStructure yearStructure;

    @OneToMany(mappedBy = "grade", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<StudentCoursePlanCourse> courses = new ArrayList<StudentCoursePlanCourse>();

    protected StudentCoursePlanGrade() {
    }

    public StudentCoursePlanGrade(Integer gradeLevel, CoursePlanYearStructure yearStructure) {
        this.gradeLevel = gradeLevel;
        this.yearStructure = yearStructure;
    }

    public StudentCoursePlan getCoursePlan() {
        return coursePlan;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public CoursePlanYearStructure getYearStructure() {
        return yearStructure;
    }

    public List<StudentCoursePlanCourse> getCourses() {
        return courses;
    }

    void attachToPlan(StudentCoursePlan coursePlan) {
        this.coursePlan = coursePlan;
    }

    public void replaceCourses(List<StudentCoursePlanCourse> replacementCourses) {
        this.courses.clear();
        if (replacementCourses == null) {
            return;
        }
        for (StudentCoursePlanCourse course : replacementCourses) {
            if (course == null) {
                continue;
            }
            course.attachToGrade(this);
            this.courses.add(course);
        }
    }
}
