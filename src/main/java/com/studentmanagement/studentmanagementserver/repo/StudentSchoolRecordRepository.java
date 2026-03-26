package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.enums.SchoolType;
import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentSchoolRecordRepository extends JpaRepository<StudentSchoolRecord, Long> {

    List<StudentSchoolRecord> findByStudent_IdOrderByIdAsc(Long studentId);

    List<StudentSchoolRecord> findByStudent_IdInOrderByStudent_IdAscIdAsc(Collection<Long> studentIds);

    boolean existsByStudent_IdAndSchoolTypeAndSchoolNameAndStartTimeAndEndTime(Long studentId,
                                                                                SchoolType schoolType,
                                                                                String schoolName,
                                                                                LocalDate startTime,
                                                                                LocalDate endTime);

    Optional<StudentSchoolRecord> findByIdAndStudent_Id(Long schoolRecordId, Long studentId);

    long deleteByStudent_Id(Long studentId);
}
