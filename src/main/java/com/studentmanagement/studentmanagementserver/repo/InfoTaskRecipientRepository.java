package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.InfoTaskCategory;
import com.studentmanagement.studentmanagementserver.domain.task.InfoTaskRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfoTaskRecipientRepository extends JpaRepository<InfoTaskRecipient, Long> {

    interface InfoTaskRecipientStudentIdView {
        Long getInfoTaskId();
        Long getStudentId();
    }

    @EntityGraph(attributePaths = {
            "infoTask",
            "infoTask.publishedByTeacher",
            "infoTask.publishedByTeacher.user",
            "student",
            "student.user"
    })
    @Query("select r from InfoTaskRecipient r " +
            "where r.student.id = :studentId " +
            "and (:category is null or r.infoTask.category = :category) " +
            "and (:unreadOnly = false or r.read = false) " +
            "and (:keyword is null " +
            "or lower(r.infoTask.title) like concat('%', :keyword, '%') " +
            "or lower(r.infoTask.content) like concat('%', :keyword, '%') " +
            "or lower(coalesce(r.infoTask.tagsText, '')) like concat('%', :keyword, '%')) " +
            "and (:tag is null or lower(coalesce(r.infoTask.tagsText, '')) like concat('%', :tag, '%')) " +
            "order by r.infoTask.updatedAt desc, r.infoTask.id desc")
    Page<InfoTaskRecipient> findStudentInfos(@Param("studentId") Long studentId,
                                             @Param("category") InfoTaskCategory category,
                                             @Param("tag") String tag,
                                             @Param("keyword") String keyword,
                                             @Param("unreadOnly") boolean unreadOnly,
                                             Pageable pageable);

    @EntityGraph(attributePaths = {
            "infoTask",
            "infoTask.publishedByTeacher",
            "infoTask.publishedByTeacher.user",
            "student",
            "student.user"
    })
    Optional<InfoTaskRecipient> findByInfoTask_IdAndStudent_Id(Long infoTaskId, Long studentId);

    @EntityGraph(attributePaths = {
            "student",
            "student.user"
    })
    List<InfoTaskRecipient> findByInfoTask_Id(Long infoTaskId);

    @Query("select r.student.id from InfoTaskRecipient r " +
            "where r.infoTask.id = :infoTaskId " +
            "order by r.student.id asc")
    List<Long> findStudentIdsByInfoTaskId(@Param("infoTaskId") Long infoTaskId);

    @Query("select r.infoTask.id as infoTaskId, r.student.id as studentId from InfoTaskRecipient r " +
            "where r.infoTask.id in :infoTaskIds " +
            "order by r.infoTask.id asc, r.student.id asc")
    List<InfoTaskRecipientStudentIdView> findRecipientStudentIdsByInfoTaskIds(@Param("infoTaskIds") List<Long> infoTaskIds);
}
