package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.extracurricular.StudentExtracurricularActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudentExtracurricularActivityRepository extends JpaRepository<StudentExtracurricularActivity, Long> {
    List<StudentExtracurricularActivity> findByTracking_IdOrderByIdAsc(Long trackingId);

    List<StudentExtracurricularActivity> findByTracking_IdIn(Collection<Long> trackingIds);

    void deleteByTracking_Id(Long trackingId);
}
