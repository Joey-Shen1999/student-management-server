package com.studentmanagement.studentmanagementserver.api.dto;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;

public class RegisterResponse {
    private Long userId;
    private UserRole role;
    private Long studentId;
    private Long teacherId;

    public RegisterResponse() {}

    public RegisterResponse(Long userId, UserRole role, Long studentId, Long teacherId) {
        this.userId = userId;
        this.role = role;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}
