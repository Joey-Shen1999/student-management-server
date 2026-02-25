package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.enums.TeacherStudentStatus;
import com.studentmanagement.studentmanagementserver.domain.teacher.TeacherStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, Long> {

    boolean existsByTeacher_IdAndStudent_IdAndStatus(Long teacherId,
                                                     Long studentId,
                                                     TeacherStudentStatus status);
}
