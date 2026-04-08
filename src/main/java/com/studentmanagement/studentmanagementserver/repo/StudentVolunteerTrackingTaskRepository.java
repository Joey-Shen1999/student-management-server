package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.volunteer.StudentVolunteerTrackingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentVolunteerTrackingTaskRepository extends JpaRepository<StudentVolunteerTrackingTask, Long> {
    List<StudentVolunteerTrackingTask> findByTracking_IdOrderByIdAsc(Long trackingId);

    void deleteByTracking_Id(Long trackingId);
}
