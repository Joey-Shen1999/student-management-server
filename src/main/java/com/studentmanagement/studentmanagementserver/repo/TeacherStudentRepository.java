package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, Long> {

    boolean existsByStudent_Id(Long studentId);

    boolean existsByTeacher_IdAndStudent_Id(Long teacherId, Long studentId);

    boolean existsByTeacher_IdAndStudent_IdAndStatus(Long teacherId,
                                                     Long studentId,
                                                     TeacherStudentStatus status);

    Optional<TeacherStudent> findTopByTeacher_IdAndStudent_IdOrderByIdDesc(Long teacherId, Long studentId);

    @Query("select ts from TeacherStudent ts " +
            "join fetch ts.student s " +
            "join fetch s.user " +
            "where ts.teacher.id = :teacherId")
    List<TeacherStudent> findByTeacherIdWithStudentAndUser(@Param("teacherId") Long teacherId);

    @Query("select distinct s from TeacherStudent ts " +
            "join ts.student s " +
            "join fetch s.user " +
            "where ts.teacher.id = :teacherId and ts.status = :status")
    List<Student> findDistinctStudentsByTeacherIdAndStatusWithUser(@Param("teacherId") Long teacherId,
                                                                   @Param("status") TeacherStudentStatus status);

    @Query("select distinct s from TeacherStudent ts " +
            "join ts.student s " +
            "join fetch s.user " +
            "where ts.teacher.user.id = :teacherUserId and ts.status = :status")
    List<Student> findDistinctStudentsByTeacherUserIdAndStatusWithUser(@Param("teacherUserId") Long teacherUserId,
                                                                       @Param("status") TeacherStudentStatus status);

    boolean existsByTeacher_User_IdAndStudent_IdAndStatus(Long teacherUserId,
                                                          Long studentId,
                                                          TeacherStudentStatus status);
}
