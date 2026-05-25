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

    @Query("select a " +
            "from GraduationApplication a " +
            "join fetch a.student s " +
            "join fetch s.user u " +
            "join fetch a.university un " +
            "join fetch a.program p " +
            "where un.id = :universityId " +
            "and u.status = com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus.ACTIVE " +
            "order by lower(s.lastName) asc, lower(s.firstName) asc, s.id asc, a.sortOrder asc, a.id asc")
    List<GraduationApplication> findActiveStudentApplicationsByUniversityId(@Param("universityId") Long universityId);

    @Query("select a.university.id as universityId, " +
            "a.university.name as universityName, " +
            "count(distinct a.student.id) as studentCount, " +
            "count(a.id) as applicationCount " +
            "from GraduationApplication a " +
            "join a.student s " +
            "join s.user u " +
            "where u.status = com.studentmanagement.studentmanagementserver.domain.enums.UserAccountStatus.ACTIVE " +
            "group by a.university.id, a.university.name " +
            "order by count(distinct a.student.id) desc, count(a.id) desc, lower(a.university.name) asc")
    List<UniversityApplicationSummaryView> summarizeActiveApplicationsByUniversity();

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

    interface UniversityApplicationSummaryView {
        Long getUniversityId();

        String getUniversityName();

        Long getStudentCount();

        Long getApplicationCount();
    }
}
