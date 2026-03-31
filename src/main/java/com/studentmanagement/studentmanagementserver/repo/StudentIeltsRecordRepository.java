package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.ielts.StudentIeltsRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentIeltsRecordRepository extends JpaRepository<StudentIeltsRecord, Long> {
    List<StudentIeltsRecord> findByIeltsModule_Id(Long ieltsModuleId);

    List<StudentIeltsRecord> findByIeltsModule_IdOrderByTestDateDescIdDesc(Long ieltsModuleId);

    long deleteByIeltsModule_Id(Long ieltsModuleId);
}
