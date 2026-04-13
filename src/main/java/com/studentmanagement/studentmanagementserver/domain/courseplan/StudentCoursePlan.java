package com.studentmanagement.studentmanagementserver.domain.courseplan;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.student.Student;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_course_plan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_course_plan_student_id", columnNames = "student_id")
        })
public class StudentCoursePlan extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "current_grade_level")
    private Integer currentGradeLevel;

    @Column(name = "grade13_enabled", nullable = false)
    private boolean grade13Enabled;

    @OneToMany(mappedBy = "coursePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @javax.persistence.OrderBy("gradeLevel ASC")
    private List<StudentCoursePlanGrade> grades = new ArrayList<StudentCoursePlanGrade>();

    protected StudentCoursePlan() {
    }

    public StudentCoursePlan(Student student) {
        this.student = student;
        this.grade13Enabled = false;
    }

    public Student getStudent() {
        return student;
    }

    public Integer getCurrentGradeLevel() {
        return currentGradeLevel;
    }

    public boolean isGrade13Enabled() {
        return grade13Enabled;
    }

    public List<StudentCoursePlanGrade> getGrades() {
        return grades;
    }

    public void overwrite(Integer currentGradeLevel, boolean grade13Enabled) {
        this.currentGradeLevel = currentGradeLevel;
        this.grade13Enabled = grade13Enabled;
    }

    public void replaceGrades(List<StudentCoursePlanGrade> replacementGrades) {
        this.grades.clear();
        if (replacementGrades == null) {
            return;
        }
        for (StudentCoursePlanGrade grade : replacementGrades) {
            if (grade == null) {
                continue;
            }
            grade.attachToPlan(this);
            this.grades.add(grade);
        }
    }
}
