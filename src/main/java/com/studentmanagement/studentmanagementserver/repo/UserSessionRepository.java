package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.user.UserSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update UserSession s set s.revokedAt = :revokedAt where s.user.id = :userId and s.revokedAt is null")
    int revokeAllActiveSessions(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

}
