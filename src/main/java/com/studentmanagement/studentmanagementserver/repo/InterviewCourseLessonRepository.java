package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.InterviewCourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewCourseLessonRepository extends JpaRepository<InterviewCourseLesson, Long> {
    List<InterviewCourseLesson> findByPlan_IdOrderByLessonNumberAsc(Long planId);
    List<InterviewCourseLesson> findByPlan_IdInOrderByPlan_IdAscLessonNumberAsc(Collection<Long> planIds);
    Optional<InterviewCourseLesson> findByIdAndPlan_Id(Long lessonId, Long planId);
}
