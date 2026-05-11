package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.university.UniversityAspiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UniversityAspirationRepository extends JpaRepository<UniversityAspiration, Long> {
    List<UniversityAspiration> findByStudent_IdOrderBySortOrderAscIdAsc(Long studentId);

    List<UniversityAspiration> findByStudent_IdAndIdIn(Long studentId, List<Long> ids);

    @Query("select coalesce(max(a.sortOrder), 0) from UniversityAspiration a where a.student.id = :studentId")
    Integer findMaxSortOrderByStudentId(@Param("studentId") Long studentId);
}
