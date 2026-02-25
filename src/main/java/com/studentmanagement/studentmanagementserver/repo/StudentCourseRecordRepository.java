package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentCourseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCourseRecordRepository extends JpaRepository<StudentCourseRecord, Long> {

    List<StudentCourseRecord> findByStudent_IdOrderByIdAsc(Long studentId);

    long deleteByStudent_Id(Long studentId);
}
