package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByStudent_Id(Long studentId);

    boolean existsByStudent_Id(Long studentId);

    @Query("select distinct sp from StudentProfile sp " +
            "join fetch sp.student s " +
            "left join fetch sp.serviceItems " +
            "where s.id in :studentIds")
    List<StudentProfile> findByStudentIdsWithStudent(@Param("studentIds") List<Long> studentIds);
}
