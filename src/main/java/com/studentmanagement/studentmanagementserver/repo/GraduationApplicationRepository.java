package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.graduation.GraduationApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GraduationApplicationRepository extends JpaRepository<GraduationApplication, Long> {

    List<GraduationApplication> findByStudent_IdOrderBySortOrderAscIdAsc(Long studentId);

    List<GraduationApplication> findByStudent_IdAndIdIn(Long studentId, List<Long> ids);

    @Modifying
    void deleteByStudent_Id(Long studentId);

    @Query("select coalesce(max(a.sortOrder), 0) from GraduationApplication a where a.student.id = :studentId")
    Integer findMaxSortOrderByStudentId(@Param("studentId") Long studentId);

    @Query("select a.student.id as studentId, count(a.id) as applicationCount " +
            "from GraduationApplication a " +
            "where a.student.id in :studentIds " +
            "group by a.student.id")
    List<StudentApplicationCountView> countByStudentIds(@Param("studentIds") List<Long> studentIds);

    interface StudentApplicationCountView {
        Long getStudentId();

        Long getApplicationCount();
    }
}
