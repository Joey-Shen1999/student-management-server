package com.studentmanagement.studentmanagementserver.domain.user;

import com.studentmanagement.studentmanagementserver.domain.common.BaseEntity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_token_hash", columnList = "tokenHash", unique = true),
                @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_sessions_expires_at", columnList = "expiresAt")
        }
)
public class UserSession extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    protected UserSession() {
    }

    public UserSession(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public boolean isActiveAt(LocalDateTime at) {
        return revokedAt == null && expiresAt.isAfter(at);
    }

    public void revokeNow() {
        this.revokedAt = LocalDateTime.now();
    }
}
