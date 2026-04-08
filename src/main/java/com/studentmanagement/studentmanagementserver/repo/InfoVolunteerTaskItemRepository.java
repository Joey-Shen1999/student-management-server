package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.InfoVolunteerTaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfoVolunteerTaskItemRepository extends JpaRepository<InfoVolunteerTaskItem, Long> {
    List<InfoVolunteerTaskItem> findByInfoTask_IdOrderByIdAsc(Long infoTaskId);

    List<InfoVolunteerTaskItem> findByInfoTask_IdInOrderByInfoTask_IdAscIdAsc(List<Long> infoTaskIds);

    void deleteByInfoTask_Id(Long infoTaskId);
}
