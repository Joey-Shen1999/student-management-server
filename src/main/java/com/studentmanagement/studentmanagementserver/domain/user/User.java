package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_username", columnList = "username", unique = true)
)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private LocalDateTime lastLoginAt;

    /**
     * First login should force password change when true.
     * Default false for normal accounts.
     */
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    protected User() {}

    public User(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.mustChangePassword = false;
    }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
