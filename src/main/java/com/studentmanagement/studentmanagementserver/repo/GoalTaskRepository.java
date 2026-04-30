package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.GoalTask;
import com.studentmanagement.studentmanagementserver.domain.task.GoalTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalTaskRepository extends JpaRepository<GoalTask, Long> {

    @EntityGraph(attributePaths = {
            "assignedStudent",
            "assignedStudent.user",
            "assignedByTeacher",
            "assignedByTeacher.user"
    })
    @Query("select g from GoalTask g " +
            "where g.assignedStudent.id = :studentId " +
            "and (:status is null or g.status = :status) " +
            "and (:keyword is null " +
            "or lower(g.title) like concat('%', :keyword, '%') " +
            "or lower(g.description) like concat('%', :keyword, '%') " +
            "or lower(coalesce(g.progressNote, '')) like concat('%', :keyword, '%'))")
    Page<GoalTask> findStudentGoals(@Param("studentId") Long studentId,
                                    @Param("status") GoalTaskStatus status,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @EntityGraph(attributePaths = {
            "assignedStudent",
            "assignedStudent.user",
            "assignedByTeacher",
            "assignedByTeacher.user"
    })
    @Query("select g from GoalTask g " +
            "where (:assignedByTeacherId is null or g.assignedByTeacher.id = :assignedByTeacherId) " +
            "and (:studentId is null or g.assignedStudent.id = :studentId) " +
            "and (:status is null or g.status = :status) " +
            "and (:keyword is null " +
            "or lower(g.title) like concat('%', :keyword, '%') " +
            "or lower(g.description) like concat('%', :keyword, '%') " +
            "or lower(coalesce(g.progressNote, '')) like concat('%', :keyword, '%'))")
    Page<GoalTask> findTeacherGoals(@Param("assignedByTeacherId") Long assignedByTeacherId,
                                    @Param("studentId") Long studentId,
                                    @Param("status") GoalTaskStatus status,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @EntityGraph(attributePaths = {
            "assignedStudent",
            "assignedStudent.user",
            "assignedByTeacher",
            "assignedByTeacher.user"
    })
    @Query("select g from GoalTask g where g.id = :goalTaskId")
    Optional<GoalTask> findByIdWithRelations(@Param("goalTaskId") Long goalTaskId);

    @EntityGraph(attributePaths = {
            "assignedStudent",
            "assignedStudent.user",
            "assignedByTeacher",
            "assignedByTeacher.user"
    })
    List<GoalTask> findByTaskGroupIdOrderByIdAsc(String taskGroupId);

    boolean existsByTaskGroupId(String taskGroupId);

    boolean existsByTaskGroupIdAndAssignedStudent_IdAndIdNot(String taskGroupId, Long assignedStudentId, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GoalTask g where g.taskGroupId = :taskGroupId")
    int deleteByTaskGroupId(@Param("taskGroupId") String taskGroupId);
}
