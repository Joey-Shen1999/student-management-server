package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.GraduationApplicationPortalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GraduationApplicationPortalCredentialRepository
        extends JpaRepository<GraduationApplicationPortalCredential, Long> {

    Optional<GraduationApplicationPortalCredential> findByStudent_IdAndUniversity_Id(Long studentId, Long universityId);

    List<GraduationApplicationPortalCredential> findByStudent_Id(Long studentId);
}
