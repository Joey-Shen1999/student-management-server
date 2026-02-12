package com.studentmanagement.studentmanagementserver.api.dto;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;

public class LoginResponse {
    private Long userId;
    private UserRole role;
    private Long studentId; // role=STUDENT 才有
    private Long teacherId; // role=TEACHER 才有

    public LoginResponse(Long userId, UserRole role, Long studentId, Long teacherId) {
        this.userId = userId;
        this.role = role;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    public Long getUserId() { return userId; }
    public UserRole getRole() { return role; }
    public Long getStudentId() { return studentId; }
    public Long getTeacherId() { return teacherId; }
}
