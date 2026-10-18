package com.studentmanagement.studentmanagementserver.domain.graduation;

public class GraduationApplicationInterviewStudentDto {
    private Long studentId;
    private String studentName;
    private String username;
    private Long universityId;
    private String universityName;
    private Integer graduationYear;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }
    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
}
