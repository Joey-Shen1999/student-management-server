package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.InterviewCoursePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InterviewCoursePlanRepository extends JpaRepository<InterviewCoursePlan, Long> {
    Optional<InterviewCoursePlan> findByStudent_Id(Long studentId);

    @Query("select p from InterviewCoursePlan p join fetch p.student s join fetch s.user u " +
            "where p.enabled = true and u.status = " +
            "com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus.ACTIVE " +
            "order by lower(s.lastName), lower(s.firstName), s.id")
    List<InterviewCoursePlan> findEnabledForActiveStudents();

}
