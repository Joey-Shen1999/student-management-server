package com.studentmanagement.studentmanagementserver.domain.courseplan;

import java.util.List;

public class StudentCoursePlanGradeDto {
    private Integer gradeLevel;
    private String yearStructure;
    private List<StudentCoursePlanCourseDto> courses;

    public StudentCoursePlanGradeDto() {
    }

    public StudentCoursePlanGradeDto(Integer gradeLevel,
                                     String yearStructure,
                                     List<StudentCoursePlanCourseDto> courses) {
        this.gradeLevel = gradeLevel;
        this.yearStructure = yearStructure;
        this.courses = courses;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getYearStructure() {
        return yearStructure;
    }

    public void setYearStructure(String yearStructure) {
        this.yearStructure = yearStructure;
    }

    public List<StudentCoursePlanCourseDto> getCourses() {
        return courses;
    }

    public void setCourses(List<StudentCoursePlanCourseDto> courses) {
        this.courses = courses;
    }
}
