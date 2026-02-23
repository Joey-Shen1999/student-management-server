package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherPasswordResetAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherPasswordResetAuditLogRepository extends JpaRepository<TeacherPasswordResetAuditLog, Long> {
}
