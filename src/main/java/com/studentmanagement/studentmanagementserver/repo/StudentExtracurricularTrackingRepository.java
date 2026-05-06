package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.extracurricular.StudentExtracurricularTracking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentExtracurricularTrackingRepository extends JpaRepository<StudentExtracurricularTracking, Long> {

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "updatedByTeacher",
            "updatedByTeacher.user"
    })
    Optional<StudentExtracurricularTracking> findByStudent_Id(Long studentId);

    @EntityGraph(attributePaths = {"student"})
    List<StudentExtracurricularTracking> findByStudent_IdIn(Collection<Long> studentIds);
}
