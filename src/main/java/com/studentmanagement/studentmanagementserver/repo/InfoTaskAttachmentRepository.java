package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.task.InfoTaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InfoTaskAttachmentRepository extends JpaRepository<InfoTaskAttachment, Long> {

    List<InfoTaskAttachment> findByInfoTask_IdOrderByCreatedAtAscIdAsc(Long infoTaskId);

    List<InfoTaskAttachment> findByInfoTask_IdInOrderByInfoTask_IdAscIdAsc(List<Long> infoTaskIds);

    Optional<InfoTaskAttachment> findByIdAndInfoTask_Id(Long attachmentId, Long infoTaskId);

    void deleteByInfoTask_Id(Long infoTaskId);
}
