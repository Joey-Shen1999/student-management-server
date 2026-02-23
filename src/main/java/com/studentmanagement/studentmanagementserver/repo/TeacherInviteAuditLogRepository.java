package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherInviteAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherInviteAuditLogRepository extends JpaRepository<TeacherInviteAuditLog, Long> {
}
