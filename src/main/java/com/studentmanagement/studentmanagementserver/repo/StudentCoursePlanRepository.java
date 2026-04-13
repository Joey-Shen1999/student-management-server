package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.courseplan.StudentCoursePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentCoursePlanRepository extends JpaRepository<StudentCoursePlan, Long> {

    Optional<StudentCoursePlan> findByStudent_Id(Long studentId);
}
