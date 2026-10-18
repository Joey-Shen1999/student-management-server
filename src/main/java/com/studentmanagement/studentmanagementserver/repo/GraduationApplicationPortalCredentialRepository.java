package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.GraduationApplicationPortalCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GraduationApplicationPortalCredentialRepository
        extends JpaRepository<GraduationApplicationPortalCredential, Long> {

    Optional<GraduationApplicationPortalCredential> findByStudent_IdAndUniversity_Id(Long studentId, Long universityId);

    List<GraduationApplicationPortalCredential> findByStudent_Id(Long studentId);

    @Query("select c from GraduationApplicationPortalCredential c " +
            "join fetch c.student s join fetch s.user u join fetch c.university un " +
            "where c.videoInterviewRequired = true " +
            "and u.status = com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus.ACTIVE " +
            "and exists (select a.id from GraduationApplication a " +
            "where a.student = s and a.university = un) " +
            "order by lower(s.lastName), lower(s.firstName), s.id, lower(un.name)")
    List<GraduationApplicationPortalCredential> findActiveInterviewRequirements();
}
