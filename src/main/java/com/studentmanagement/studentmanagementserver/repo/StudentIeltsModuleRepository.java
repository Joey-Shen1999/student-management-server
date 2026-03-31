package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.ielts.StudentIeltsModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentIeltsModuleRepository extends JpaRepository<StudentIeltsModule, Long> {
    Optional<StudentIeltsModule> findByStudent_Id(Long studentId);
}
