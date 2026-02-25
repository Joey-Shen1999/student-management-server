package com.studentmanagement.studentmanagementserver.api.dto;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;

public class RegisterRequest {
    private String username;
    private String password;
    private UserRole role; // STUDENT / TEACHER

    // student minimal
    private String firstName;
    private String lastName;
    private String preferredName;

    // teacher minimal
    private String displayName;

    // optional invite for student registration
    private String inviteToken;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPreferredName() { return preferredName; }
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
}
