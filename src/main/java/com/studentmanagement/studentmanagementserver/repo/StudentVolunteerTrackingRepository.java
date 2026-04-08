package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.volunteer.StudentVolunteerTracking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentVolunteerTrackingRepository extends JpaRepository<StudentVolunteerTracking, Long> {

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "updatedByTeacher",
            "updatedByTeacher.user"
    })
    Optional<StudentVolunteerTracking> findByStudent_Id(Long studentId);

    @EntityGraph(attributePaths = {"student"})
    List<StudentVolunteerTracking> findByStudent_IdIn(Collection<Long> studentIds);
}
