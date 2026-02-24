package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;
import com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
    private UserAccountStatus status = UserAccountStatus.ACTIVE;

    private LocalDateTime statusUpdatedAt;

    private Long statusUpdatedBy;

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
        this.status = UserAccountStatus.ACTIVE;
        this.mustChangePassword = false;
    }

    @PrePersist
    void ensureDefaults() {
        if (this.status == null) {
            this.status = UserAccountStatus.ACTIVE;
        }
    }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserAccountStatus getStatus() {
        return status == null ? UserAccountStatus.ACTIVE : status;
    }

    public void setStatus(UserAccountStatus status) {
        this.status = status == null ? UserAccountStatus.ACTIVE : status;
    }

    public void updateStatus(UserAccountStatus status, Long operatorUserId) {
        this.status = status == null ? UserAccountStatus.ACTIVE : status;
        this.statusUpdatedAt = LocalDateTime.now();
        this.statusUpdatedBy = operatorUserId;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public Long getStatusUpdatedBy() {
        return statusUpdatedBy;
    }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
