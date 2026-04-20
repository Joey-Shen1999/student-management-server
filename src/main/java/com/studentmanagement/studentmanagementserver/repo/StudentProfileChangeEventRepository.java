package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentProfileChangeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileChangeEventRepository extends JpaRepository<StudentProfileChangeEvent, Long> {

    Page<StudentProfileChangeEvent> findByStudentId(Long studentId, Pageable pageable);
}
