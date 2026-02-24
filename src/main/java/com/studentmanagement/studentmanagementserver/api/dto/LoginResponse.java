package com.studentmanagement.studentmanagementserver.api.dto;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;

public class LoginResponse {

    private Long userId;
    private UserRole role;
    private Long studentId;
    private Long teacherId;
    private boolean mustChangePassword;
    private String accessToken;
    private String tokenType;
    private String tokenExpiresAt;

    public LoginResponse(Long userId,
                         UserRole role,
                         Long studentId,
                         Long teacherId,
                         boolean mustChangePassword,
                         String accessToken,
                         String tokenType,
                         String tokenExpiresAt) {
        this.userId = userId;
        this.role = role;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.mustChangePassword = mustChangePassword;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getTokenExpiresAt() {
        return tokenExpiresAt;
    }
}
