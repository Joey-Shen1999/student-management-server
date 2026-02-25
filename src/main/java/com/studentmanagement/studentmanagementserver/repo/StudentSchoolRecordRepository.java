package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudentSchoolRecordRepository extends JpaRepository<StudentSchoolRecord, Long> {

    List<StudentSchoolRecord> findByStudent_IdOrderByIdAsc(Long studentId);

    boolean existsByStudent_IdAndSchoolTypeAndSchoolNameAndStartTimeAndEndTime(Long studentId,
                                                                                SchoolType schoolType,
                                                                                String schoolName,
                                                                                LocalDate startTime,
                                                                                LocalDate endTime);

    long deleteByStudent_Id(Long studentId);
}
