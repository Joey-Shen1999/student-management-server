package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.serviceprogress.ServiceProgressRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceProgressRecordRepository extends JpaRepository<ServiceProgressRecord, Long> {
    List<ServiceProgressRecord> findByStudent_IdOrderByAppointmentTimeDescCreatedAtDescIdDesc(Long studentId);
}
