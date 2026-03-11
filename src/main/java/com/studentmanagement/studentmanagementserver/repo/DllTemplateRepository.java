package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.DllTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DllTemplateRepository extends JpaRepository<DllTemplate, Long> {

    @EntityGraph(attributePaths = {
            "createdByTeacher",
            "createdByTeacher.user"
    })
    @Query("select t from DllTemplate t where t.id = :templateId")
    Optional<DllTemplate> findByIdWithCreator(@Param("templateId") Long templateId);
}
