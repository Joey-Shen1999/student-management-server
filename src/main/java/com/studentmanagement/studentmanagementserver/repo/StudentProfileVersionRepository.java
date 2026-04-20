package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentProfileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileVersionRepository extends JpaRepository<StudentProfileVersion, Long> {

    Optional<StudentProfileVersion> findTopByStudentIdOrderByProfileVersionDescIdDesc(Long studentId);
}
