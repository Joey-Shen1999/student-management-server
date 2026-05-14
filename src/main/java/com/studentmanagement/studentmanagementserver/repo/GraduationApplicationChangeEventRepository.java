package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.GraduationApplicationChangeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationApplicationChangeEventRepository extends JpaRepository<GraduationApplicationChangeEvent, Long> {

    Page<GraduationApplicationChangeEvent> findByStudentId(Long studentId, Pageable pageable);
}
