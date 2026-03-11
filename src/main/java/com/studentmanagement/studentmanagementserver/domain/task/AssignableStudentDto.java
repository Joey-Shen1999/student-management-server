package com.studentmanagement.studentmanagementserver.domain.task;

public class AssignableStudentDto {
    private Long studentId;
    private String studentName;
    private String username;

    public AssignableStudentDto(Long studentId, String studentName, String username) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.username = username;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getUsername() {
        return username;
    }
}
