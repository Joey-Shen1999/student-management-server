package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.ielts.StudentIeltsManualStatusAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentIeltsManualStatusAuditLogRepository extends JpaRepository<StudentIeltsManualStatusAuditLog, Long> {
}
