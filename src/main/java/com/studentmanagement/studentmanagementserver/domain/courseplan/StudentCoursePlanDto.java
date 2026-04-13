package com.studentmanagement.studentmanagementserver.domain.courseplan;

import java.util.List;

public class StudentCoursePlanDto {
    private Integer currentGradeLevel;
    private boolean grade13Enabled;
    private List<StudentCoursePlanGradeDto> grades;

    public StudentCoursePlanDto() {
    }

    public StudentCoursePlanDto(Integer currentGradeLevel,
                                boolean grade13Enabled,
                                List<StudentCoursePlanGradeDto> grades) {
        this.currentGradeLevel = currentGradeLevel;
        this.grade13Enabled = grade13Enabled;
        this.grades = grades;
    }

    public Integer getCurrentGradeLevel() {
        return currentGradeLevel;
    }

    public void setCurrentGradeLevel(Integer currentGradeLevel) {
        this.currentGradeLevel = currentGradeLevel;
    }

    public boolean isGrade13Enabled() {
        return grade13Enabled;
    }

    public void setGrade13Enabled(boolean grade13Enabled) {
        this.grade13Enabled = grade13Enabled;
    }

    public List<StudentCoursePlanGradeDto> getGrades() {
        return grades;
    }

    public void setGrades(List<StudentCoursePlanGradeDto> grades) {
        this.grades = grades;
    }
}
