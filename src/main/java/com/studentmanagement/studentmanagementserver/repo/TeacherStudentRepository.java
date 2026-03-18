package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, Long> {

    boolean existsByStudent_Id(Long studentId);

    boolean existsByTeacher_IdAndStudent_IdAndStatus(Long teacherId,
                                                     Long studentId,
                                                     TeacherStudentStatus status);

    @Query("select distinct s from TeacherStudent ts " +
            "join ts.student s " +
            "join fetch s.user " +
            "where ts.teacher.id = :teacherId and ts.status = :status")
    List<Student> findDistinctStudentsByTeacherIdAndStatusWithUser(@Param("teacherId") Long teacherId,
                                                                   @Param("status") TeacherStudentStatus status);
}
