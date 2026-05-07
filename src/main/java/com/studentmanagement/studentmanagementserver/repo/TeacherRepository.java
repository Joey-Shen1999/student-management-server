package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.teacher.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUser_Id(Long userId);

    @Query("select t from Teacher t join fetch t.user")
    List<Teacher> findAllWithUser();

    @Query("select t from Teacher t join fetch t.user where t.advisorEnabled = true order by t.name asc, t.id asc")
    List<Teacher> findEnabledAdvisorsWithUser();

}
