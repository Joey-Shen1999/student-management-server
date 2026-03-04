package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentSchoolTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentSchoolTranscriptRepository extends JpaRepository<StudentSchoolTranscript, Long> {

    List<StudentSchoolTranscript> findBySchoolRecord_IdOrderByUploadedAtDescIdDesc(Long schoolRecordId);

    List<StudentSchoolTranscript> findBySchoolRecord_IdInOrderBySchoolRecord_IdAscUploadedAtDescIdDesc(
            Collection<Long> schoolRecordIds
    );

    Optional<StudentSchoolTranscript> findByIdAndSchoolRecord_Id(Long transcriptId, Long schoolRecordId);

    long deleteBySchoolRecord_Id(Long schoolRecordId);

    long countBySchoolRecord_Id(Long schoolRecordId);
}
