package com.studentmanagement.studentmanagementserver.repo;

import com.studentmanagement.studentmanagementserver.domain.student.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudent_IdOrderByUploadedAtDescIdDesc(Long studentId);

    Optional<StudentDocument> findByIdAndStudent_Id(Long documentId, Long studentId);

    List<StudentDocument> findByLinkedIdentityFileId(Long linkedIdentityFileId);

    List<StudentDocument> findByLinkedSchoolTranscriptId(Long linkedSchoolTranscriptId);
}
