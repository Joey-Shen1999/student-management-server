package com.studentmanagement.studentmanagementserver.domain.courseplan;

public class StudentCoursePlanCourseDto {
    private String id;
    private String courseCode;
    private String status;
    private Integer mark;
    private String semester;
    private Integer sortOrder;

    public StudentCoursePlanCourseDto() {
    }

    public StudentCoursePlanCourseDto(String id,
                                      String courseCode,
                                      String status,
                                      Integer mark,
                                      String semester,
                                      Integer sortOrder) {
        this.id = id;
        this.courseCode = courseCode;
        this.status = status;
        this.mark = mark;
        this.semester = semester;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMark() {
        return mark;
    }

    public void setMark(Integer mark) {
        this.mark = mark;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
