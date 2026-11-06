package com.studentmanagement.studentmanagementserver.domain.graduation;

import java.util.List;

public class InterviewCoursePlanDto {
    private Long studentId;
    private String studentName;
    private String username;
    private Integer graduationYear;
    private String schoolProvince;
    private String schoolCity;
    private String schoolCountry;
    public String getSchoolProvince() { return schoolProvince; }
    public String getSchoolCity() { return schoolCity; }
    public String getSchoolCountry() { return schoolCountry; }
    public void assignSchool(String province, String city, String country) {
        schoolProvince = province; schoolCity = city; schoolCountry = country;
    }

    private boolean enabled;
    private Long teacherId;
    private String teacherName;
    private String teacherUsername;
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getTeacherUsername() { return teacherUsername; }
    public void assignTeacher(Long id, String name, String username) {
        teacherId = id; teacherName = name; teacherUsername = username;
    }
    private List<InterviewCourseLessonDto> lessons;
    private List<InterviewCourseRequirementDto> requirements;
    private boolean graduationStageEnabled;
    private List<InterviewCourseRequirementDto> engineeringApplications;

    public InterviewCoursePlanDto(Long studentId, String studentName, String username,
                                  Integer graduationYear, boolean enabled,
                                  List<InterviewCourseLessonDto> lessons,
                                  List<InterviewCourseRequirementDto> requirements,
                                  boolean graduationStageEnabled,
                                  List<InterviewCourseRequirementDto> engineeringApplications) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.username = username;
        this.graduationYear = graduationYear;
        this.enabled = enabled;
        this.lessons = lessons;
        this.requirements = requirements;
        this.graduationStageEnabled = graduationStageEnabled;
        this.engineeringApplications = engineeringApplications;
    }

    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getUsername() { return username; }
    public Integer getGraduationYear() { return graduationYear; }
    public boolean isEnabled() { return enabled; }
    public List<InterviewCourseLessonDto> getLessons() { return lessons; }
    public List<InterviewCourseRequirementDto> getRequirements() { return requirements; }
    public boolean isGraduationStageEnabled() { return graduationStageEnabled; }
    public List<InterviewCourseRequirementDto> getEngineeringApplications() { return engineeringApplications; }
}
