package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface StudentInviteRepository extends JpaRepository<StudentInvite, Long> {

    boolean existsByInviteToken(String inviteToken);

    @Query("select i from StudentInvite i join fetch i.teacher where i.inviteToken = :inviteToken")
    Optional<StudentInvite> findByInviteTokenWithTeacher(@Param("inviteToken") String inviteToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StudentInvite i join fetch i.teacher where i.inviteToken = :inviteToken")
    Optional<StudentInvite> findByInviteTokenForUpdate(@Param("inviteToken") String inviteToken);
}
