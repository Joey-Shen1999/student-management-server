package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.InterviewCourseRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewCourseRequirementRepository extends JpaRepository<InterviewCourseRequirement, Long> {
    List<InterviewCourseRequirement> findByPlan_IdOrderByIdAsc(Long planId);
    List<InterviewCourseRequirement> findByPlan_IdInOrderByPlan_IdAscIdAsc(Collection<Long> planIds);
    Optional<InterviewCourseRequirement> findByIdAndPlan_Id(Long id, Long planId);
}
