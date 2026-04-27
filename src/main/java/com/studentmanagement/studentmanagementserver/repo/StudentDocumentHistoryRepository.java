package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentDocumentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentDocumentHistoryRepository extends JpaRepository<StudentDocumentHistory, Long> {

    Page<StudentDocumentHistory> findByStudentId(Long studentId, Pageable pageable);

    Optional<StudentDocumentHistory> findFirstByDocumentIdAndActionOrderByActionAtDescIdDesc(
            Long documentId,
            String action
    );
}
