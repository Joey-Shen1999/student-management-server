package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.InfoTask;
import com.studentmanagement.studentmanagementserver.domain.task.InfoTaskCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfoTaskRepository extends JpaRepository<InfoTask, Long> {

    @EntityGraph(attributePaths = {
            "publishedByTeacher",
            "publishedByTeacher.user"
    })
    @Query("select i from InfoTask i " +
            "where (:publishedByTeacherId is null or i.publishedByTeacher.id = :publishedByTeacherId) " +
            "and (:category is null or i.category = :category) " +
            "and (:keyword is null " +
            "or lower(i.title) like concat('%', :keyword, '%') " +
            "or lower(i.content) like concat('%', :keyword, '%') " +
            "or lower(coalesce(i.tagsText, '')) like concat('%', :keyword, '%')) " +
            "and (:tag is null or lower(coalesce(i.tagsText, '')) like concat('%', :tag, '%')) " +
            "order by i.updatedAt desc, i.id desc")
    Page<InfoTask> findTeacherInfos(@Param("publishedByTeacherId") Long publishedByTeacherId,
                                    @Param("category") InfoTaskCategory category,
                                    @Param("tag") String tag,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
}
