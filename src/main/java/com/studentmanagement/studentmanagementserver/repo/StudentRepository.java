package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser_Id(Long userId);

    @Query("select s from Student s join fetch s.user")
    List<Student> findAllWithUser();

    @Query("select s from Student s left join fetch s.teacher where s.id = :studentId")
    Optional<Student> findByIdWithTeacher(@Param("studentId") Long studentId);

}
