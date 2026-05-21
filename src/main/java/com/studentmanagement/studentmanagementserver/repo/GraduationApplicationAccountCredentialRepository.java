package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.GraduationApplicationAccountCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GraduationApplicationAccountCredentialRepository
        extends JpaRepository<GraduationApplicationAccountCredential, Long> {

    Optional<GraduationApplicationAccountCredential> findByStudent_Id(Long studentId);
}
