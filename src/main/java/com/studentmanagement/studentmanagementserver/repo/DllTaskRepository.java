package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.DllTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DllTaskRepository extends JpaRepository<DllTask, Long> {

    @EntityGraph(attributePaths = {
            "template",
            "assignedStudent",
            "createdByTeacher"
    })
    @Query("select d from DllTask d " +
            "where (:createdByTeacherId is null or d.createdByTeacher.id = :createdByTeacherId) " +
            "order by d.updatedAt desc, d.id desc")
    Page<DllTask> findTeacherDllTasks(@Param("createdByTeacherId") Long createdByTeacherId, Pageable pageable);
}
